package net.blay09.mods.bmc.gui.chat;

import com.google.common.base.Strings;
import net.blay09.mods.bmc.ChatViewManager;
import net.blay09.mods.bmc.event.ClientChatEvent;
import net.blay09.mods.bmc.event.TabCompletionEvent;
import net.blay09.mods.bmc.event.ChatComponentClickEvent;
import net.blay09.mods.bmc.event.ChatComponentHoverEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiChatExt extends GuiChat {

	public GuiChatExt(String defaultText) {
		super(defaultText);
	}

	@Override
	public void initGui() {
		String oldText = inputField != null ? inputField.getText() : null;
		super.initGui();
		inputField.width = inputField.width - 36;
		if(!Strings.isNullOrEmpty(oldText)) {
			inputField.setText(oldText);
		}
	}

	@Override
	public void sendChatMessage(String message, boolean addToSentMessages) {
		ClientChatEvent event = new ClientChatEvent(message);
		if (ChatViewManager.getActiveView().getOutgoingPrefix() != null) {
			event.setMessage(ChatViewManager.getActiveView().getOutgoingPrefix() + event.getMessage());
		}
		String newMessage;
		if(MinecraftForge.EVENT_BUS.post(event)) {
			newMessage = null;
		} else {
			newMessage = event.getMessage();
		}
		if(!Strings.isNullOrEmpty(newMessage)) {
			if(addToSentMessages) {
				// Store the originally typed message, not the potentially prefixed one.
				mc.ingameGUI.getChatGUI().addToSentMessages(message);
			}
			super.sendChatMessage(newMessage, false);
		}
	}

	@Override
	public void setCompletions(String... newCompletions) {
		String input = inputField.getText().substring(0, inputField.getCursorPosition());
		BlockPos pos = tabCompleter.getTargetBlockPos();
		List<String> list = new ArrayList<>();
		Collections.addAll(list, newCompletions);
		MinecraftForge.EVENT_BUS.post(new TabCompletionEvent(Side.CLIENT, Minecraft.getMinecraft().thePlayer, input.split(" ")[0], pos, pos != null, list));
		super.setCompletions(list.toArray(new String[list.size()]));
	}

	@Override
	protected void handleComponentHover(ITextComponent component, int x, int y) {
		if(!MinecraftForge.EVENT_BUS.post(new ChatComponentHoverEvent(component, x, y))) {
			super.handleComponentHover(component, x, y);
		}
	}

	@Override
	protected boolean handleComponentClick(ITextComponent component) {
		return (component != null && MinecraftForge.EVENT_BUS.post(new ChatComponentClickEvent(component))) || super.handleComponentClick(component);
	}
}
