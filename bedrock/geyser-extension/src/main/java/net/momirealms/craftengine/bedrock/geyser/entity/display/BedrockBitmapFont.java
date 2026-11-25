package net.momirealms.craftengine.bedrock.geyser.entity.display;

import java.util.BitSet;

enum BedrockBitmapFont {
    LEGACY(""),
    EXTENDED("ÃÏÝýĄąĆćČčĎďĘęĚěĞĹĺĽľŁłŃńŇňŐőŔŕŘřŚśŠšŤťŮůŰűŹźŻżŽ"
            + "ΆΈΉΊΌΎΏΐΑΒΔΕΖΗΙΚΛΜΝΞΟΠΡΤΥΧΨΪΫάέήίΰγεζηθικλνξορςυ"
            + "φχψωϊϋόύώЁЄІЇАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабв"
            + "гдежзийклмнопрстуфхцчшщъыьэюяёєіїҐґ„…");

    private static final int PROTOCOL_26_60 = 2225;
    private static final String LEGACY_NON_ASCII =
            "¡£§©ª«¬®°±²·º»¼½¿ÀÁÂÄÅÆÇÈÉÊËÍÑÓÔÕÖ×ØÚÜßàáâãäåæçè"
                    + "éêëìíîïñòóôõö÷øùúûüÿğİıŒœŞşŴŵžƒȇΓΘΣΦΩαβδμπστ–—‘’"
                    + "“”ⁿ™∅∈∙√∞∩≈≡≤≥⌠⌡─│┌┐└┘├┤┬┴┼═║╒╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡╢╣╤"
                    + "╥╦╧╨╩╪╫╬▀▄█▌▐░▒▓■";

    private final BitSet characters = new BitSet();

    BedrockBitmapFont(String additionalCharacters) {
        this.characters.set(' ', '~' + 1);
        LEGACY_NON_ASCII.chars().forEach(this.characters::set);
        additionalCharacters.chars().forEach(this.characters::set);
    }

    static BedrockBitmapFont forProtocol(int protocolVersion) {
        return protocolVersion >= PROTOCOL_26_60 ? EXTENDED : LEGACY;
    }

    float shadowOffset(String renderedLine) {
        // 按最终发给 DebugText 的整行选字形。§ 和有效格式码均在位图表内，不会触发字体回退。
        for (int index = 0; index < renderedLine.length();) {
            int codepoint = renderedLine.codePointAt(index);
            if (!this.characters.get(codepoint)) {
                return 0.5f;
            }
            index += Character.charCount(codepoint);
        }
        return 1.0f;
    }
}
