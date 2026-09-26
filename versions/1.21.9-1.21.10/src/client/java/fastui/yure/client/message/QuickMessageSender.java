package fastui.yure.client.message;

import fastui.yure.config.QuickMessage;
import fastui.yure.config.QuickMessageTemplate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/** 统一处理快捷消息的聊天与指令发送。 */
public final class QuickMessageSender {
    private QuickMessageSender() {
    }

    public static boolean send(QuickMessage message) {
        return message != null && send(message.content());
    }

    public static boolean send(String content) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler connection = client.getNetworkHandler();
        if (content == null || content.isBlank() || client.player == null || client.world == null || connection == null) {
            return false;
        }
        content = QuickMessageTemplate.resolve(content, context(client));
        if (content.isBlank()) {
            return false;
        }
        if (isCommand(content)) {
            String command = content.substring(1);
            if (command.isBlank()) {
                return false;
            }
            connection.sendChatCommand(command);
        } else {
            connection.sendChatMessage(content);
        }
        return true;
    }

    public static boolean isCommand(String content) {
        return content != null && content.startsWith("/");
    }

    private static QuickMessageTemplate.Context context(MinecraftClient client) {
        var player = client.player;
        var position = player.getBlockPos();
        var precise = player.getEntityPos();
        return new QuickMessageTemplate.Context(player.getName().getString(), position.getX(), position.getY(),
                position.getZ(), precise.x, precise.y, precise.z, client.world.getRegistryKey().getValue().toString());
    }
}
