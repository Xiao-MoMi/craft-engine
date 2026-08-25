package net.momirealms.craftengine.core.attribute.damage;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class DamageRuleTable {
    private final Map<Key, RulesByVictim> rulesByCause;

    public DamageRuleTable(Map<Key, RulesByVictim> rulesByCause) {
        this.rulesByCause = Map.copyOf(rulesByCause);
    }

    @Nullable
    public DamageRule find(DamageEvent event) {
        RulesByVictim rules = this.rulesByCause.get(event.source().type());
        if (rules == null) {
            return null;
        }
        return rules.find(event.victim().id());
    }

    public record RulesByVictim(@Nullable DamageRule defaultRule, Map<Key, DamageRule> rules) {

        public RulesByVictim {
            rules = Map.copyOf(rules);
        }

        @Nullable
        public DamageRule find(Key victim) {
            return this.rules.getOrDefault(victim, this.defaultRule);
        }
    }
}
