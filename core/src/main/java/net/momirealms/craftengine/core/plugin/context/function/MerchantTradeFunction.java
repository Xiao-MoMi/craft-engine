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
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
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
    private final BiFunction<Player, Context, List<MerchantOffer>> offers;

    private MerchantTradeFunction(List<Condition<CTX>> predicates,
                                  @Nullable PlayerSelector<CTX> selector,
                                  String title,
                                  BiFunction<Player, Context, List<MerchantOffer>> offers) {
        super(predicates);
        this.title = title;
        this.selector = selector;
        this.offers = offers;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                CraftEngine.instance().guiManager().openMerchant(it, this.title == null ? null : AdventureHelper.deserialize(this.title, ctx), this.offers.apply(it, ctx));
            });
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                CraftEngine.instance().guiManager().openMerchant(viewer, this.title == null ? null : AdventureHelper.deserialize(this.title, relationalContext), this.offers.apply(viewer, relationalContext));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, MerchantTradeFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, MerchantTradeFunction<CTX>> {
        private static final String[] OFFERS = ConfigKeys.of("offer(s)");
        private static final String[] COST_1 = ConfigKeys.of("cost_1");
        private static final String[] COST_2 = ConfigKeys.of("cost_2");
        private static final String[] EXP = ConfigKeys.of("exp|experience");

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public MerchantTradeFunction<CTX> create(ConfigSection section) {
            List<TempOffer> merchantOffers = section.getSectionList(OFFERS, s -> {
                TempItem cost1 = s.getNonNullValue(COST_1, ConfigConstants.ARGUMENT_SECTION, this::parseTempItem);
                TempItem cost2 = s.getValue(COST_2, this::parseTempItem);
                TempItem result = s.getNonNullValue("result", ConfigConstants.ARGUMENT_SECTION, this::parseTempItem);
                NumberProvider exp = s.getNumber(EXP, ConfigConstants.CONSTANT_ZERO);
                return new TempOffer(cost1, cost2, result, exp);
            });

            return new MerchantTradeFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getString("title"),
                    (player, context) -> {
                        List<MerchantOffer> offers = new ArrayList<>(merchantOffers.size());
                        for (TempOffer offer : merchantOffers) {
                            Item cost1 = offer.cost1.build(player, context);
                            Optional cost2 = Optional.ofNullable(offer.cost2).map(it -> it.build(player, context));
                            Item result = offer.result.build(player, context);
                            offers.add(new MerchantOffer(cost1, cost2, result, false, 0, Integer.MAX_VALUE, offer.exp.getInt(context), 0, 0, 0));
                        }
                        return offers;
                    });
        }

        private static final String[] ITEM = ConfigKeys.of("item|id");
        private static final String[] COUNT = ConfigKeys.of("count|amount");
        private static final String[] COMPONENT = ConfigKeys.of("component(s)");
        private static final String[] NBT = ConfigKeys.of("nbt|tags");

        private TempItem parseTempItem(ConfigValue value) {
            if (value.is(Map.class)) {
                ConfigSection section = value.getAsSection();
                Key itemId = section.getNonNullIdentifier(ITEM);
                NumberProvider count = section.getNumber(COUNT, ConfigConstants.CONSTANT_ONE);
                ComponentsProcessor componentsProcessor = null;
                TagsProcessor tagsProcessor = null;
                if (VersionHelper.COMPONENT_RELEASE) {
                    ConfigSection components = section.getSection(COMPONENT);
                    if (components != null) {
                        componentsProcessor = new ComponentsProcessor(components);
                    }
                } else {
                    ConfigSection nbt = section.getSection(NBT);
                    if (nbt != null) {
                        tagsProcessor = new TagsProcessor(nbt);
                    }
                }
                return new TempItem(itemId, count, componentsProcessor, tagsProcessor);
            } else {
                return new TempItem(value.getAsIdentifier(), ConfigConstants.CONSTANT_ONE, null, null);
            }
        }

        public record TempOffer(TempItem cost1, TempItem cost2, TempItem result, NumberProvider exp) {
        }

        public record TempItem(Key id, NumberProvider count, ComponentsProcessor components, TagsProcessor nbt) {

            public Item build(Player player, Context context) {
                Item item = Item.byId(this.id, player);
                if (item == null) {
                    item = Item.byId(ItemKeys.STONE, player);
                    assert item != null;
                    item.itemNameComponent(Component.text(this.id.asString()).color(NamedTextColor.RED));
                } else {
                    if (this.components != null) {
                        ItemBuildContext empty = ItemBuildContext.empty();
                        empty.setItem(item);
                        this.components.apply(empty);
                    }
                    if (this.nbt != null) {
                        ItemBuildContext empty = ItemBuildContext.empty();
                        empty.setItem(item);
                        this.nbt.apply(empty);
                    }
                }
                item.count(this.count.getInt(context));
                return item;
            }
        }
    }
}