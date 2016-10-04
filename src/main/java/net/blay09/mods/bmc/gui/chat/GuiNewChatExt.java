package net.blay09.mods.bmc.gui.chat;

import com.google.common.collect.Lists;
import net.blay09.mods.bmc.ChatManager;
import net.blay09.mods.bmc.ChatTweaks;
import net.blay09.mods.bmc.ChatTweaksConfig;
import net.blay09.mods.bmc.ChatViewManager;
import net.blay09.mods.bmc.chat.ChatChannel;
import net.blay09.mods.bmc.chat.ChatView;
import net.blay09.mods.bmc.chat.ChatMessage;
import net.blay09.mods.bmc.chat.TextRenderRegion;
import net.blay09.mods.bmc.image.ChatImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiNewChatExt extends GuiNewChat {

	public static class WrappedChatLine {
		private final int timeCreated;
		private final ChatMessage message;
		private final ITextComponent component;
		private final String cleanText;
		private final TextRenderRegion[] regions;
		private final List<ChatImage> images;
		private final boolean alternateBackground;

		public WrappedChatLine(int timeCreated, ChatMessage message, ITextComponent component, String cleanText, TextRenderRegion[] regions, @Nullable List<ChatImage> images, boolean alternateBackground) {
			this.timeCreated = timeCreated;
			this.message = message;
			this.component = component;
			this.cleanText = cleanText;
			this.regions = regions;
			this.images = images;
			this.alternateBackground = alternateBackground;
		}
	}

	private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-OR#]");
	private static final Pattern EMOTE_PATTERN = Pattern.compile("\u00a7\\*");

	private final Minecraft mc;
	private final List<WrappedChatLine> wrappedChatLines = Lists.newArrayList();
	private FontRenderer fontRenderer;
	private boolean alternateBackground;

	public GuiNewChatExt(Minecraft mc) {
		super(mc);
		this.mc = mc;
		this.fontRenderer = mc.fontRendererObj;
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void printChatMessageWithOptionalDeletion(ITextComponent chatComponent, int chatLineId) {
		if (chatLineId == 0) {
			chatLineId = ChatManager.getNextMessageId();
		}
		ChatMessage message = new ChatMessage(chatLineId, chatComponent);
		addChatMessage(message, ChatManager.findChatChannel(message));
	}

	public void addChatMessage(ChatMessage message, ChatChannel channel) {
		List<ChatView> views = ChatViewManager.findChatViews(message, channel);
		for (ChatView view : views) {
			addChatMessageForDisplay(view.addChatLine(message), view);
		}
	}

	private void addChatMessageForDisplay(ChatMessage chatMessage, ChatView view) {
		switch (view.getMessageStyle()) {
			case Chat:
				if (view != ChatViewManager.getActiveView()) {
					view.markAsUnread(chatMessage);
				}
				int chatWidth = MathHelper.floor_float((float) this.getChatWidth() / this.getChatScale());
				List<ITextComponent> wrappedList = GuiUtilRenderComponents.splitText(chatMessage.getChatComponent(), chatWidth, this.mc.fontRendererObj, false, false);
				boolean isChatOpen = this.getChatOpen();
				int colorIndex = 0;
				int emoteIndex = 0;
				for (ITextComponent chatLine : wrappedList) {
					if (isChatOpen && this.scrollPos > 0) {
						this.isScrolled = true;
						this.scroll(1);
					}
					String formattedText = chatLine.getFormattedText();
					String[] split = formattedText.split("\u00a7#");
					TextRenderRegion[] regions = new TextRenderRegion[split.length];
					for (int i = 0; i < regions.length; i++) {
						if(i > 0) {
							colorIndex++;
						}
						regions[i] = new TextRenderRegion(split[i], chatMessage.getRGBColor(colorIndex));
					}
					String cleanText = FORMATTING_CODE_PATTERN.matcher(chatLine.getUnformattedText()).replaceAll("");
					Matcher matcher = EMOTE_PATTERN.matcher(cleanText);
					List<ChatImage> images = null;
					if(chatMessage.hasImages()) {
						images = Lists.newArrayList();
						while (matcher.find()) {
							ChatImage image = chatMessage.getImage(emoteIndex);
							if(image != null) {
								image.setIndex(matcher.start());
								images.add(image);
							}
							emoteIndex++;
						}
					}
					this.wrappedChatLines.add(0, new WrappedChatLine(mc.ingameGUI.getUpdateCounter(), chatMessage, chatLine, cleanText, regions, images, alternateBackground));
				}
				while (this.wrappedChatLines.size() > 100) {
					this.wrappedChatLines.remove(this.wrappedChatLines.size() - 1);
				}
				alternateBackground = !alternateBackground;
				break;
			case Side:
				ChatTweaks.getSideChatHandler().addMessage(chatMessage);
				break;
			case Bottom:
				ChatTweaks.getBottomChatHandler().setMessage(chatMessage);
				break;
		}
	}

	@Override
	public void clearChatMessages() {
		// TODO implement me
	}

	@Override
	public void refreshChat() {
		// TODO implement me
	}

	@Override
	public void drawChat(int updateCounter) {
		if (this.mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
			int lineSpacing = ChatTweaksConfig.lineSpacing;
			float chatOpacity = this.mc.gameSettings.chatOpacity * 0.9f + 0.1f;
			int wrappedChatLinesCount = wrappedChatLines.size();
			if (wrappedChatLinesCount > 0) {
				boolean isChatOpen = this.getChatOpen();
				float chatScale = this.getChatScale();
				int chatWidth = MathHelper.ceiling_float_int((float) this.getChatWidth() / chatScale);
				GlStateManager.pushMatrix();
				GlStateManager.translate(2f, 8f, 0f);
				GlStateManager.scale(chatScale, chatScale, 1f);

				int maxVisibleLines = this.getLineCount();
				int drawnLinesCount = 0;
				for (int lineIdx = 0; lineIdx + this.scrollPos < this.wrappedChatLines.size() && lineIdx < maxVisibleLines; lineIdx++) {
					WrappedChatLine chatLine = this.wrappedChatLines.get(lineIdx + this.scrollPos);
					int lifeTime = updateCounter - chatLine.timeCreated;
					if (lifeTime < 200 || isChatOpen) {
						int alpha = 255;
						if (!isChatOpen) {
							float percentage = (1f - (float) lifeTime / 200f) * 10f;
							percentage = MathHelper.clamp_float(percentage, 0f, 1f);
							percentage = percentage * percentage;
							alpha = (int) (255f * percentage);
						}
						alpha = (int) ((float) alpha * chatOpacity);
						if (alpha > 3) {
							int x = 0;
							int y = -lineIdx * (fontRenderer.FONT_HEIGHT + lineSpacing);
							if (chatLine.message.hasBackgroundColor()) {
								drawRect(-2, y - fontRenderer.FONT_HEIGHT + lineSpacing / 2, chatWidth + 4, y + (int) Math.ceil((float) lineSpacing / 2f), (chatLine.message.getBackgroundColor() & 0x00FFFFFF) + (alpha << 24));
							} else {
								drawRect(-2, y - fontRenderer.FONT_HEIGHT - lineSpacing / 2, chatWidth + 4, y + (int) Math.ceil((float) lineSpacing / 2f), ((chatLine.alternateBackground ? ChatTweaksConfig.backgroundColor1 : ChatTweaksConfig.backgroundColor2) & 0x00FFFFFF) + (alpha << 24));
							}
							GlStateManager.enableBlend();
							for (TextRenderRegion region : chatLine.regions) {
								x = fontRenderer.drawString(region.getText(), x, y - fontRenderer.FONT_HEIGHT + 1, (region.getColor() & 0x00FFFFFF) + (alpha << 24), true);
							}
							if(chatLine.images != null) {
								for (ChatImage image : chatLine.images) {
									int spaceWidth = Minecraft.getMinecraft().fontRendererObj.getCharWidth(' ') * image.getSpaces();
									float scale = image.getScale();
									int renderOffset = fontRenderer.getStringWidth(chatLine.cleanText.substring(0, image.getIndex()));
									int renderWidth = (int) (image.getWidth() * scale);
									int renderHeight = (int) (image.getHeight() * scale);
									int renderX = -2 + renderOffset + spaceWidth / 2 - renderWidth / 2;
									int renderY = y - Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT / 2 - renderHeight / 2;
									GlStateManager.pushMatrix();
									GlStateManager.scale(scale, scale, 1f);
									image.draw((int) (renderX / scale), (int) (renderY / scale), 255);
									GlStateManager.popMatrix();
								}
							}
							GlStateManager.disableAlpha();
							GlStateManager.disableBlend();
						}
						drawnLinesCount++;
					}
				}

				// Render the scroll bar, if necessary
				if (isChatOpen) {
					GlStateManager.translate(-3f, 0f, 0f);
					int fullHeight = wrappedChatLinesCount * fontRenderer.FONT_HEIGHT + wrappedChatLinesCount;
					int drawnHeight = drawnLinesCount * fontRenderer.FONT_HEIGHT + drawnLinesCount;
					int scrollY = this.scrollPos * drawnHeight / wrappedChatLinesCount;
					int scrollHeight = drawnHeight * drawnHeight / fullHeight;
					if (fullHeight != drawnHeight) {
						int alpha = scrollY > 0 ? 0xAA : 0x60;
						int color = this.isScrolled ? 0xCC3333 : 0x3333AA;
						drawRect(0, -scrollY, 2, -scrollY - scrollHeight, color + (alpha << 24));
						drawRect(2, -scrollY, 1, -scrollY - scrollHeight, 0xCCCCCC + (alpha << 24));
					}
				}

				GlStateManager.popMatrix();
			}
		}
	}

	@Nullable
	public ITextComponent getChatComponent(int mouseX, int mouseY) {
		if (!this.getChatOpen()) {
			return null;
		}
		ScaledResolution resolution = new ScaledResolution(this.mc);
		int scaleFactor = resolution.getScaleFactor();
		float chatScale = this.getChatScale();
		int x = mouseX / scaleFactor - 2;
		int y = mouseY / scaleFactor - 40;
		x = MathHelper.floor_float((float) x / chatScale);
		y = MathHelper.floor_float((float) y / chatScale);
		if (x >= 0 && y >= 0) {
			int lineCount = Math.min(this.getLineCount(), this.wrappedChatLines.size());
			if (x <= MathHelper.floor_float((float) this.getChatWidth() / this.getChatScale()) && y < (fontRenderer.FONT_HEIGHT + ChatTweaksConfig.lineSpacing) * lineCount + lineCount) {
				int clickedIndex = y / (fontRenderer.FONT_HEIGHT + ChatTweaksConfig.lineSpacing) + this.scrollPos;
				if (clickedIndex >= 0 && clickedIndex < this.wrappedChatLines.size()) {
					WrappedChatLine chatLine = this.wrappedChatLines.get(clickedIndex);
					int width = 0;
					for (ITextComponent component : chatLine.component) {
						if (component instanceof TextComponentString) {
							width += fontRenderer.getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(((TextComponentString) component).getText(), false));
							if (width > x) {
								return component;
							}
						}
					}
				}
			}
		}
		return null;
	}

}
