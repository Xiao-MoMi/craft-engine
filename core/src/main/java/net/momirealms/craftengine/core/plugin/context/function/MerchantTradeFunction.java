package net.momirealms.craftengine.core.plugin.context.function;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.processor.ComponentsProcessor;
import net.momirealms.craftengine.core.item.processor.TagsProcessor;
import net.momirealms.craftengine.core.item.trade.MerchantOffer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public final class MerchantTradeFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String title;
    private final PlayerSelector<CTX> selector;
    private final BiFunction<Player, Context, List<MerchantOffer<?>>> offers;

    private MerchantTradeFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector, String title, BiFunction<Player, Context, List<MerchantOffer<?>>> offers) {
        super(predicates);
        this.title = title;
        this.selector = selector;
        this.offers = offers;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                CraftEngine.instance().guiManager().openMerchant(it, this.title == null ? null : AdventureHelper.miniMessage().deserialize(this.title, ctx.tagResolvers()), this.offers.apply(it, ctx));
            });
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                CraftEngine.instance().guiManager().openMerchant(viewer, this.title == null ? null : AdventureHelper.miniMessage().deserialize(this.title, relationalContext.tagResolvers()), this.offers.apply(viewer, relationalContext));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, MerchantTradeFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, MerchantTradeFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public MerchantTradeFunction<CTX> create(ConfigSection section) {
            List<TempOffer> merchantOffers = section.parseSectionList(s -> {
                TempItem cost1 = s.getNonNull(o -> parseTempItem(o, section.assemblePath("cost_1")), ConfigConstants.ARGUMENT_SECTION, "cost_1", "cost-1");
                TempItem cost2 = s.getOrDefault(o -> parseTempItem(o, section.assemblePath("cost_2")), (TempItem) null, "cost_2", "cost-2");
                TempItem result = s.getNonNull(o -> parseTempItem(o, section.assemblePath("result")), ConfigConstants.ARGUMENT_SECTION, "result");
                NumberProvider exp = s.getOrDefault(NumberProviders::fromObject, NumberProviders.direct(0), "exp", "experience");
                return new TempOffer(cost1, cost2, result, exp);
            }, "offers", "offer");

            return new MerchantTradeFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getString("title"),
                    (player, context) -> {
                        List<MerchantOffer<?>> offers = new ArrayList<>(merchantOffers.size());
                        for (TempOffer offer : merchantOffers) {
                            Item cost1 = offer.cost1.build(player, context);
                            Optional cost2 = Optional.ofNullable(offer.cost2).map(it -> it.build(player, context));
                            Item result = offer.result.build(player, context);
                            offers.add(new MerchantOffer<>(cost1, cost2, result, false, 0, Integer.MAX_VALUE, offer.exp.getInt(context), 0, 0, 0));
                        }
                        return offers;
                    });
        }

        private TempItem parseTempItem(Object object, String path) {
            if (object instanceof Map) {
                ConfigSection innerSection = ConfigSection.of(path, MiscUtils.castToMap(object));
                Key itemId = innerSection.getNonNullIdentifier("item", "id");
                NumberProvider count = Optional.ofNullable(innerSection.get("count", "amount")).map(NumberProviders::fromObject).orElseGet(() -> NumberProviders.direct(1));
                ComponentsProcessor componentsProcessor = null;
                TagsProcessor tagsProcessor = null;
                if (VersionHelper.COMPONENT_RELEASE) {
                    ConfigSection components = innerSection.getSection("components", "component");
                    if (components != null) {
                        componentsProcessor = new ComponentsProcessor(components.values());
                    }
                } else {
                    ConfigSection nbt = innerSection.getSection("nbt", "tags");
                    if (nbt != null) {
                        tagsProcessor = new TagsProcessor(nbt.values());
                    }
                }
                return new TempItem(itemId, count, componentsProcessor, tagsProcessor);
            } else {
                return new TempItem(Key.of(object.toString()), NumberProviders.direct(1), null, null);
            }
        }

        public record TempOffer(TempItem cost1, TempItem cost2, TempItem result, NumberProvider exp) {
        }

        public record TempItem(Key id, NumberProvider count, ComponentsProcessor components, TagsProcessor nbt) {

            public Item<Object> build(Player player, Context context) {
                Item<Object> item = CraftEngine.instance().itemManager().createWrappedItem(this.id, player);
                if (item == null) {
                    item = CraftEngine.instance().itemManager().createWrappedItem(ItemKeys.STONE, player);
                    assert item != null;
                    item.itemNameComponent(Component.text(this.id.asString()).color(NamedTextColor.RED));
                } else {
                    if (this.components != null) {
                        this.components.apply(item, ItemBuildContext.empty());
                    }
                    if (this.nbt != null) {
                        this.nbt.apply(item, ItemBuildContext.empty());
                    }
                }
                item.count(this.count.getInt(context));
                return item;
            }
        }
    }
}