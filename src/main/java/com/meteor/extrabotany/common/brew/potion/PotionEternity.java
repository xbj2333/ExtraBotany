package com.meteor.extrabotany.common.brew.potion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.meteor.extrabotany.common.brew.ModPotions;
import com.meteor.extrabotany.common.lib.LibPotionsName;

import net.minecraft.command.CommandEffect;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PotionEternity extends PotionMod{
	private static final Map<UUID, Long> AUTHORIZED_UNTIL = new HashMap<>();
	private static final int MIN_AUTHORIZATION_BUFFER = 20;
	private static final int COMMAND_DEFAULT_SECONDS = 30;
	private static final int COMMAND_MAX_SECONDS = 1000000;

	public PotionEternity() {
		super(LibPotionsName.ETERNITY, false, 0XDAA520, 1);
		MinecraftForge.EVENT_BUS.register(this);
		setBeneficial();
	}

	public static void addAuthorizedEffect(EntityPlayer player, int duration, int amplifier) {
		if (player == null || player.world.isRemote) {
			return;
		}
		authorize(player, duration);
		player.addPotionEffect(new PotionEffect(ModPotions.eternity, duration, amplifier));
	}

	@SubscribeEvent
	public void onUpdate(LivingUpdateEvent event) {
		EntityLivingBase living = event.getEntityLiving();
		if (living instanceof EntityPlayer && !living.world.isRemote) {
			EntityPlayer player = (EntityPlayer) living;
			if (!player.isCreative() && player.isPotionActive(ModPotions.eternity) && !isAuthorized(player)) {
				player.removePotionEffect(ModPotions.eternity);
				return;
			}
		}
		if(living.isPotionActive(ModPotions.eternity) && living.getActivePotionEffect(ModPotions.eternity).getDuration() < 115){
			living.motionY = 0;
			living.motionX*=0.25F;
			living.motionZ*=0.25F;
		}
	}

	@SubscribeEvent
	public void onCommand(CommandEvent event) {
		if (!(event.getCommand() instanceof CommandEffect) || event.getParameters().length < 2) {
			return;
		}

		String[] args = event.getParameters();
		Potion potion = Potion.getPotionFromResourceLocation(args[1]);
		if (potion != ModPotions.eternity) {
			try {
				potion = Potion.getPotionById(Integer.parseInt(args[1]));
			} catch (NumberFormatException ignored) {
				potion = null;
			}
		}
		if (potion != ModPotions.eternity || (args.length >= 3 && "0".equals(args[2]))) {
			return;
		}

		int seconds = COMMAND_DEFAULT_SECONDS;
		if (args.length >= 3) {
			try {
				seconds = Math.max(1, Math.min(COMMAND_MAX_SECONDS, Integer.parseInt(args[2])));
			} catch (NumberFormatException ignored) {
				seconds = COMMAND_DEFAULT_SECONDS;
			}
		}

		int duration = seconds * 20;
		if (event.getSender().getServer() == null) {
			return;
		}
		for (EntityPlayerMP player : event.getSender().getServer().getPlayerList().getPlayers()) {
			if (!player.isCreative()) {
				authorize(player, duration);
			}
		}
	}
	
	@SubscribeEvent
	public void onDamageTaken(LivingHurtEvent event) {
		if(!(event.getEntityLiving() instanceof EntityPlayer))
			return;
		EntityPlayer player = (EntityPlayer) event.getEntityLiving();
		if(player.isPotionActive(ModPotions.eternity))
			event.setAmount(0F);
	}

	private static void authorize(EntityPlayer player, int duration) {
		AUTHORIZED_UNTIL.put(player.getUniqueID(),
				player.world.getTotalWorldTime() + duration + MIN_AUTHORIZATION_BUFFER);
	}

	private static boolean isAuthorized(EntityPlayer player) {
		Long authorizedUntil = AUTHORIZED_UNTIL.get(player.getUniqueID());
		if (authorizedUntil == null) {
			return false;
		}
		if (player.world.getTotalWorldTime() <= authorizedUntil) {
			return true;
		}
		AUTHORIZED_UNTIL.remove(player.getUniqueID());
		return false;
	}

}
