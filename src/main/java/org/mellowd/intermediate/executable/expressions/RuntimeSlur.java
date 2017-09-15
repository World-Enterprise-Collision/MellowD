package org.mellowd.intermediate.executable.expressions;

import org.mellowd.intermediate.functions.operations.Slurrable;
import org.mellowd.compiler.ExecutionEnvironment;

public class RuntimeSlur<T extends Slurrable> implements Expression<T> {
    private final Expression<T> toSlur;
    private final Expression<Boolean> slur;

    public RuntimeSlur(Expression<T> toSlur, Expression<Boolean> slur) {
        this.toSlur = toSlur;
        this.slur = slur;
    }

    @Override
    public T evaluate(ExecutionEnvironment environment) {
        T toSlur = this.toSlur.evaluate(environment);
        Boolean slur = this.slur.evaluate(environment);

        toSlur.setSlurred(slur);

        return toSlur;
    }
}
