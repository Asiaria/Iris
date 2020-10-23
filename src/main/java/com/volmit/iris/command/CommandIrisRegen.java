package com.volmit.iris.command;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.volmit.iris.Iris;
import com.volmit.iris.gen.scaffold.IrisWorlds;
import com.volmit.iris.util.MortarCommand;
import com.volmit.iris.util.MortarSender;
import com.volmit.iris.util.Spiraler;

public class CommandIrisRegen extends MortarCommand
{
	public CommandIrisRegen()
	{
		super("regenerate", "regen", "rg");
		setDescription("Regenerate chunks");
		requiresPermission(Iris.perm.studio);
		setCategory("Regen");
	}

	@Override
	public boolean handle(MortarSender sender, String[] args)
	{
		if(sender.isPlayer())
		{
			Player p = sender.player();
			World world = p.getWorld();
			if(!IrisWorlds.isIrisWorld(world))
			{
				sender.sendMessage("You must be in an iris world.");
				return true;
			}

			if(IrisWorlds.getProvider(world).isFailing())
			{
				sender.sendMessage("This world is in a failed state! Cannot Regenerate!");
				return true;
			}

			IrisWorlds.getProvisioned(world).clearRegeneratedLists();
			if(args.length == 0)
			{
				sender.sendMessage("Regenerating your chunk");
				IrisWorlds.getProvisioned(world).regenerate(p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ());
				return true;
			}

			try
			{
				int m = Integer.valueOf(args[0]);
				sender.sendMessage("Regenerating " + (m * m) + " Chunks Surrounding you");
				new Spiraler(m, m, (a, b) -> IrisWorlds.getProvisioned(world).regenerate(a + p.getLocation().getChunk().getX(), b + p.getLocation().getChunk().getZ())).drain();
			}
			catch(Throwable e)
			{
				sender.sendMessage("/iris regen [SIZE]");
			}

			return true;
		}
		else
		{
			sender.sendMessage("Players only.");
		}

		return true;
	}

	@Override
	protected String getArgsUsage()
	{
		return "/iris regen [size]";
	}
}