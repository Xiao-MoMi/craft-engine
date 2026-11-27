package net.momirealms.craftengine.bedrock;

public record BedrockPlayerPack(String name, String description) {
    public static final BedrockPlayerPack DEFAULT = new BedrockPlayerPack("CraftEngine Player Pack", "Dynamically generated for your Bedrock client version");
}