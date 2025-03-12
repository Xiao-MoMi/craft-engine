package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ClientLangMangerImpl implements ClientLangManager {
    private final Plugin plugin;
    private final Map<String, I18NData> i18nData = new HashMap<>();
    private final Set<String> allLang = Set.of(
            "af_za", "ar_sa", "ast_es", "az_az", "ba_ru", "bar", "be_by", "be_latn",
            "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz", "cy_gb", "da_dk",
            "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca", "en_gb", "en_nz",
            "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy", "es_ar", "es_cl",
            "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "esan", "et_ee", "eu_es",
            "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr", "fra_de", "fur_it",
            "fy_nl", "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il", "hi_in", "hn_no",
            "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is", "isv",
            "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in", "ko_kr", "ksh",
            "kw_gb", "la_la", "lb_lu", "li_li", "lmo", "lo_la", "lol_us", "lt_lt",
            "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de",
            "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pls",
            "pt_br", "pt_pt", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua", "sah_sah",
            "se_no", "sk_sk", "sl_si", "so_so", "sq_al", "sr_cs", "sr_sp", "sv_se",
            "sxu", "szl", "ta_in", "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr",
            "tt_ru", "tzo_mx", "uk_ua", "val_es", "vec_it", "vi_vn", "vp_vl", "yi_de",
            "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab"
    );

    public ClientLangMangerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        this.i18nData.clear();
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        String langId = id.value().toLowerCase(Locale.ROOT);

        Map<String, String> data = section.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.valueOf(entry.getValue())
                ));

        if ("all".equals(langId)) {
            processAllLanguages(data);
            return;
        }

        if (allLang.contains(langId)) {
            i18nData.computeIfAbsent(langId, k -> new I18NData())
                    .addTranslations(data);
        }
    }

    private void processAllLanguages(Map<String, String> section) {
        I18NData defaultData = new I18NData();
        defaultData.addTranslations(section);

        allLang.forEach(lang -> {
            I18NData data = new I18NData();
            data.translations.putAll(defaultData.translations);
            i18nData.put(lang, data);
        });
    }

    @Override
    public Map<String, I18NData> langData() {
        return Collections.unmodifiableMap(i18nData);
    }
}
