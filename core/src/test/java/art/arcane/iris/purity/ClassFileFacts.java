package art.arcane.iris.purity;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Minimal, dependency-free class-file reader. Only the structural facts the purity gate needs:
 * the annotation descriptors on the class, the supertype chain names, and the declared fields.
 * <p>
 * Reading bytes instead of reflecting is deliberate: {@code Class#getDeclaredFields()} and
 * {@code Class#getInterfaces()} resolve the types they describe, so on a classloader that hides
 * org.bukkit they throw NoClassDefFoundError before they can tell you which member was at fault.
 * The parser can name the member.
 */
public final class ClassFileFacts {
    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHODREF = 11;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_METHOD_HANDLE = 15;
    private static final int CONSTANT_METHOD_TYPE = 16;
    private static final int CONSTANT_DYNAMIC = 17;
    private static final int CONSTANT_INVOKE_DYNAMIC = 18;
    private static final int CONSTANT_MODULE = 19;
    private static final int CONSTANT_PACKAGE = 20;

    private static final int ACC_STATIC = 0x0008;
    private static final int ACC_TRANSIENT = 0x0080;

    /**
     * A declared field: raw JVM descriptor plus the generic {@code Signature} attribute when the
     * compiler emitted one.
     * <p>
     * Both matter and they fail at different moments. The descriptor is erased, and it is what
     * {@code Class#getDeclaredFields()} resolves - eagerly, for every field including transient ones -
     * so a Bukkit type there makes the whole class unloadable. The signature carries the type
     * arguments the descriptor threw away ({@code KList<BlockData>} erases to {@code KList}), and Gson
     * resolves those through {@code Field#getGenericType()} for every field it actually walks, which
     * is every non-static, non-transient field.
     */
    public record DeclaredField(String name, String descriptor, String signature, boolean isStatic, boolean isTransient) {
    }

    private final String internalName;
    private final String superName;
    private final List<String> interfaceNames;
    private final List<DeclaredField> fields;
    private final Set<String> classAnnotationDescriptors;

    private ClassFileFacts(String internalName,
                           String superName,
                           List<String> interfaceNames,
                           List<DeclaredField> fields,
                           Set<String> classAnnotationDescriptors) {
        this.internalName = internalName;
        this.superName = superName;
        this.interfaceNames = Collections.unmodifiableList(interfaceNames);
        this.fields = Collections.unmodifiableList(fields);
        this.classAnnotationDescriptors = Collections.unmodifiableSet(classAnnotationDescriptors);
    }

    public String internalName() {
        return internalName;
    }

    public String binaryName() {
        return internalName.replace('/', '.');
    }

    public String superName() {
        return superName;
    }

    public List<String> interfaceNames() {
        return interfaceNames;
    }

    public List<DeclaredField> fields() {
        return fields;
    }

    public boolean hasClassAnnotation(String descriptor) {
        return classAnnotationDescriptors.contains(descriptor);
    }

    public static ClassFileFacts read(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
            throw new IOException("Not a class file (magic " + Integer.toHexString(magic) + ")");
        }
        in.readUnsignedShort();
        in.readUnsignedShort();

        int constantPoolCount = in.readUnsignedShort();
        String[] utf8 = new String[constantPoolCount];
        int[] classNameIndex = new int[constantPoolCount];
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case CONSTANT_UTF8 -> utf8[i] = in.readUTF();
                case CONSTANT_INTEGER, CONSTANT_FLOAT, CONSTANT_FIELDREF, CONSTANT_METHODREF,
                     CONSTANT_INTERFACE_METHODREF, CONSTANT_NAME_AND_TYPE, CONSTANT_DYNAMIC,
                     CONSTANT_INVOKE_DYNAMIC -> in.readInt();
                case CONSTANT_LONG, CONSTANT_DOUBLE -> {
                    in.readLong();
                    i++;
                }
                case CONSTANT_CLASS -> classNameIndex[i] = in.readUnsignedShort();
                case CONSTANT_STRING, CONSTANT_METHOD_TYPE, CONSTANT_MODULE, CONSTANT_PACKAGE ->
                        in.readUnsignedShort();
                case CONSTANT_METHOD_HANDLE -> {
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                }
                default -> throw new IOException("Unknown constant pool tag " + tag + " at " + i);
            }
        }

        in.readUnsignedShort();
        int thisClass = in.readUnsignedShort();
        int superClass = in.readUnsignedShort();
        String internalName = utf8[classNameIndex[thisClass]];
        String superName = superClass == 0 ? null : utf8[classNameIndex[superClass]];

        int interfaceCount = in.readUnsignedShort();
        List<String> interfaceNames = new ArrayList<>(interfaceCount);
        for (int i = 0; i < interfaceCount; i++) {
            interfaceNames.add(utf8[classNameIndex[in.readUnsignedShort()]]);
        }

        int fieldCount = in.readUnsignedShort();
        List<DeclaredField> fields = new ArrayList<>(fieldCount);
        for (int i = 0; i < fieldCount; i++) {
            int accessFlags = in.readUnsignedShort();
            String name = utf8[in.readUnsignedShort()];
            String descriptor = utf8[in.readUnsignedShort()];
            String signature = readAttributes(in, utf8, null);
            fields.add(new DeclaredField(name, descriptor, signature,
                    (accessFlags & ACC_STATIC) != 0, (accessFlags & ACC_TRANSIENT) != 0));
        }

        int methodCount = in.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            in.readUnsignedShort();
            in.readUnsignedShort();
            in.readUnsignedShort();
            readAttributes(in, utf8, null);
        }

        Set<String> classAnnotations = new LinkedHashSet<>();
        readAttributes(in, utf8, classAnnotations);

        return new ClassFileFacts(internalName, superName, interfaceNames, fields, classAnnotations);
    }

    /**
     * Walks an attributes table, optionally collecting annotation descriptors, and returns the
     * {@code Signature} attribute's value when the table carries one.
     */
    private static String readAttributes(DataInputStream in, String[] utf8, Set<String> annotationSink) throws IOException {
        String signature = null;
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            String attributeName = utf8[in.readUnsignedShort()];
            int length = in.readInt();
            byte[] payload = in.readNBytes(length);
            if (payload.length != length) {
                throw new IOException("Truncated attribute " + attributeName);
            }
            if ("Signature".equals(attributeName) && payload.length == 2) {
                signature = utf8[((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF)];
            }
            if (annotationSink != null
                    && ("RuntimeVisibleAnnotations".equals(attributeName) || "RuntimeInvisibleAnnotations".equals(attributeName))) {
                collectAnnotationDescriptors(payload, utf8, annotationSink);
            }
        }
        return signature;
    }

    /**
     * Reads only the top-level annotation type descriptors out of an annotations attribute. Member
     * values are skipped structurally rather than parsed, because the gate only asks "is this class
     * annotated with @Snippet".
     */
    private static void collectAnnotationDescriptors(byte[] payload, String[] utf8, Set<String> sink) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
        int annotationCount = in.readUnsignedShort();
        for (int i = 0; i < annotationCount; i++) {
            sink.add(utf8[in.readUnsignedShort()]);
            skipElementValuePairs(in, utf8);
        }
    }

    private static void skipElementValuePairs(DataInputStream in, String[] utf8) throws IOException {
        int pairCount = in.readUnsignedShort();
        for (int i = 0; i < pairCount; i++) {
            in.readUnsignedShort();
            skipElementValue(in, utf8);
        }
    }

    private static void skipElementValue(DataInputStream in, String[] utf8) throws IOException {
        int tag = in.readUnsignedByte();
        switch (tag) {
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 's', 'c' -> in.readUnsignedShort();
            case 'e' -> {
                in.readUnsignedShort();
                in.readUnsignedShort();
            }
            case '@' -> {
                in.readUnsignedShort();
                skipElementValuePairs(in, utf8);
            }
            case '[' -> {
                int length = in.readUnsignedShort();
                for (int i = 0; i < length; i++) {
                    skipElementValue(in, utf8);
                }
            }
            default -> throw new IOException("Unknown element value tag " + (char) tag);
        }
    }
}
