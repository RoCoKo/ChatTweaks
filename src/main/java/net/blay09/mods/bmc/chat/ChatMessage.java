package net.blay09.mods.bmc.chat;

import net.blay09.mods.bmc.image.renderable.IChatRenderable;
import net.blay09.mods.bmc.image.ITooltipProvider;
import net.blay09.mods.bmc.image.ChatImage;
import net.blay09.mods.bmc.image.ChatImageDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public class ChatMessage {

    private final int id;
    private ITextComponent chatComponent;
    private int backgroundColor;
    private ChatImage[] images;
	private int[] rgbColors;
    private NBTTagCompound customData;
	private long timestamp;
	private ChatView exclusiveView;
	private boolean managed;

	public ChatMessage(int id, ITextComponent chatComponent) {
        this.id = id;
        this.chatComponent = chatComponent;
		this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

	public ITextComponent getChatComponent() {
		return chatComponent;
	}

	public void setTextComponent(ITextComponent chatComponent) {
		this.chatComponent = chatComponent;
	}

    public NBTTagCompound getCustomData() {
        return customData;
    }

    public boolean hasData() {
        return customData != null;
    }

    public boolean hasBackgroundColor() {
        return backgroundColor != 0;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

	public ChatMessage withImages(int count) {
		images = new ChatImage[count];
		return this;
	}

	@Nullable
	public ChatImage[] getImages() {
		return images;
	}

	@Nullable
	public ChatImage getImage(int index) {
		if(images == null || index < 0 || index >= images.length) {
			return null;
		}
		return images[index];
	}

	public ChatMessage setImage(int index, IChatRenderable image, ITooltipProvider tooltip) {
		setImage(index, new ChatImageDefault(index, image, tooltip));
		return this;
	}

    public ChatMessage setImage(int index, ChatImage image) {
		if(index >= 0 && index < images.length) {
			images[index] = image;
		}
		return this;
    }

	public ChatMessage withRGB(int count) {
		rgbColors = new int[count];
		for(int i = 0; i < rgbColors.length; i++) {
			rgbColors[i] = 0xFFFFFF;
		}
		return this;
	}

	public ChatMessage setRGBColor(int index, int color) {
		if(index >= 0 && index < rgbColors.length) {
			rgbColors[index] = color;
		}
		return this;
	}

	public int getRGBColor(int index) {
		if(rgbColors == null || index < 0 || index >= rgbColors.length) {
			return 0xFFFFFF;
		}
		return rgbColors[index];
	}

	public boolean hasRGBColors() {
		return rgbColors != null;
	}

    public boolean hasImages() {
        return images != null;
    }

	public long getTimestamp() {
		return timestamp;
	}

	public void setManaged(boolean managed) {
		this.managed = managed;
	}

	public boolean isManaged() {
		return managed;
	}

	public void clearImages() {
		images = null;
	}

	public ChatMessage copy() {
		ChatMessage out = new ChatMessage(id, chatComponent);
		out.backgroundColor = backgroundColor;
		if(images != null) {
			out.images = images; // TODO bad copy
		}
		if(customData != null) {
			out.customData = customData.copy();
		}
		out.timestamp = timestamp;
		return out;
	}

	public void setExclusiveView(ChatView view) {
		this.exclusiveView = view;
	}

	public boolean hasExclusiveView() {
		return exclusiveView != null;
	}

	public ChatView getExclusiveView() {
		return exclusiveView;
	}

}
