/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.shared.block;

import java.util.concurrent.Callable;

public abstract class BlockBehavior {

    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    public Object getFluidState(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return (boolean) superMethod.call();
    }

    public void onBrokenAfterFall(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public void onLand(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) throws Exception {
        return false;
    }

    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) throws Exception {
        return false;
    }

    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
    }
}
