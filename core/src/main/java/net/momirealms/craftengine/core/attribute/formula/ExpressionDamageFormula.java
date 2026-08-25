package net.momirealms.craftengine.core.attribute.formula;

import net.momirealms.craftengine.core.attribute.damage.DamageEvent;
import net.momirealms.craftengine.core.plugin.context.expression.ContextExpression;

import java.util.function.ToDoubleFunction;

public class ExpressionDamageFormula implements DamageFormula {
    public static final DamageFormulaFactory<ExpressionDamageFormula> FACTORY = args -> compile(
            args.assemblePath("expression"),
            args.getNonNullString("expression")
    );

    private final String rawExpression;
    private final ContextExpression<DamageEvent> compiled;

    private ExpressionDamageFormula(String rawExpression, ContextExpression<DamageEvent> compiled) {
        this.rawExpression = rawExpression;
        this.compiled = compiled;
    }

    public static ExpressionDamageFormula compile(String formula) {
        return new ExpressionDamageFormula(formula, compileExpression(formula));
    }

    public static ExpressionDamageFormula compile(String node, String formula) {
        return new ExpressionDamageFormula(formula, ContextExpression.precompile(
                node,
                formula,
                DamageEvent::context,
                ExpressionDamageFormula::bindVariable
        ));
    }

    private static ContextExpression<DamageEvent> compileExpression(String formula) {
        return ContextExpression.compile(formula, DamageEvent::context, ExpressionDamageFormula::bindVariable);
    }

    private static ToDoubleFunction<DamageEvent> bindVariable(String name) {
        return switch (name) {
            case "damage" -> DamageEvent::damage;
            case "is_critical" -> event -> event.source().isCritical() ? 1D : 0D;
            case "is_sweep" -> event -> event.isSweepAttack() ? 1D : 0D;
            case "attack_strength" -> DamageEvent::attackStrength;
            case "shoot_force" -> DamageEvent::shootForce;
            case "is_attack_ready" -> event -> event.isAttackReady() ? 1D : 0D;
            default -> null;
        };
    }

    @Override
    public double getValue(DamageEvent event) {
        try {
            return this.compiled.evaluate(event);
        } catch (final RuntimeException e) {
            throw new RuntimeException("Failed to evaluate damage formula: " + this.rawExpression, e);
        }
    }
}
