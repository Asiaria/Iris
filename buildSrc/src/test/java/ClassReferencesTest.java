import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassReferencesTest {
    @Test
    public void includesDeclarationOnlyFieldAndMethodTypes() {
        ClassWriter writer = createClass();
        writer.visitField(Opcodes.ACC_PUBLIC, "field", "[[Lexample/FieldType;", null, null).visitEnd();
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "method",
                "(Lexample/Argument;[I)[Lexample/Result;", null, null).visitEnd();
        writer.visitEnd();

        Set<String> references = ClassReferences.read(writer.toByteArray());
        assertTrue(references.containsAll(Set.of("example/FieldType", "example/Argument", "example/Result")));
        assertFalse(references.contains("I"));
    }

    @Test
    public void includesMemberReferenceAndMethodTypeDescriptors() {
        ClassWriter writer = createClass();
        writer.newMethod("example/Owner", "method", "(Lexample/Argument;)Lexample/Result;", false);
        writer.newConst(Type.getMethodType("(Lexample/HandleArgument;)Lexample/HandleResult;"));
        writer.visitEnd();

        Set<String> references = ClassReferences.read(writer.toByteArray());
        assertTrue(references.containsAll(Set.of("example/Owner", "example/Argument", "example/Result",
                "example/HandleArgument", "example/HandleResult")));
    }

    @Test
    public void doesNotTreatStringsOrAnnotationOnlyTypesAsLinkage() {
        ClassWriter writer = createClass();
        writer.newConst("Lexample/StringValue;");
        writer.visitAnnotation("Lexample/Annotation;", true).visitEnd();
        writer.visitEnd();

        Set<String> references = ClassReferences.read(writer.toByteArray());
        assertFalse(references.contains("example/StringValue"));
        assertFalse(references.contains("example/Annotation"));
    }

    private static ClassWriter createClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V25, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "example/Consumer", null, "java/lang/Object", null);
        return writer;
    }
}
