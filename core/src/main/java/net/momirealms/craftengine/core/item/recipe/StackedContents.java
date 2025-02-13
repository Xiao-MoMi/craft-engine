package net.momirealms.craftengine.core.item.recipe;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class StackedContents<T> {
    public final Reference2IntOpenHashMap<T> amounts = new Reference2IntOpenHashMap<>();

    public void add(T input, int count) {
        this.amounts.addTo(input, count);
    }

    void take(T input, int count) {
        int i = this.amounts.addTo(input, -count);
        if (i < count) {
            throw new IllegalStateException("Took " + count + " items, but only had " + i);
        }
    }

    void put(T input, int count) {
        this.amounts.addTo(input, count);
    }

    boolean hasAtLeast(T input, int minimum) {
        return this.amounts.getInt(input) >= minimum;
    }

    public boolean tryPick(List<? extends StackedContents.IngredientInfo<T>> ingredients) {
        return new Matcher(ingredients).tryPick(1);
    }

    @FunctionalInterface
    public interface IngredientInfo<T> {
        boolean acceptsItem(T entry);
    }

    @FunctionalInterface
    public interface Output<T> {
        void accept(T item);
    }

    List<T> getUniqueAvailableIngredientItems(List<? extends IngredientInfo<T>> ingredients) {
        List<T> list = new ArrayList<>();
        for (Reference2IntMap.Entry<T> entry : amounts.reference2IntEntrySet()) {
            if (entry.getIntValue() > 0 && anyIngredientMatches(ingredients, entry.getKey())) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    private static <T> boolean anyIngredientMatches(Iterable<? extends IngredientInfo<T>> ingredients, T item) {
        for (IngredientInfo<T> ingredientInfo : ingredients) {
            if (ingredientInfo.acceptsItem(item)) {
                return true;
            }
        }
        return false;
    }

    public class Matcher {
        private final List<? extends IngredientInfo<T>> ingredients;
        private final int ingredientCount;
        private final List<T> items;
        private final int itemCount;
        private final BitSet data;
        private final IntList path = new IntArrayList();

        public Matcher(List<? extends IngredientInfo<T>> ingredients) {
            this.ingredients = ingredients;
            this.ingredientCount = ingredients.size();
            this.items = getUniqueAvailableIngredientItems(ingredients);
            this.itemCount = this.items.size();
            this.data = new BitSet(this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount());
            this.setInitialConnections();
        }

        private void setInitialConnections() {
            for (int i = 0; i < this.ingredientCount; i++) {
                IngredientInfo<T> ingredientInfo = this.ingredients.get(i);
                for (int j = 0; j < this.itemCount; j++) {
                    if (ingredientInfo.acceptsItem(this.items.get(j))) {
                        this.setConnection(j, i);
                    }
                }
            }
        }

        @Nullable
        private IntList tryAssigningNewItem(int min) {
            this.clearAllVisited();
            for (int i = 0; i < this.itemCount; i++) {
                if (hasAtLeast(this.items.get(i), min)) {
                    IntList intList = this.findNewItemAssignmentPath(i);
                    if (intList != null) {
                        return intList;
                    }
                }
            }
            return null;
        }

        @Nullable
        private IntList findNewItemAssignmentPath(int itemIndex) {
            this.path.clear();
            this.visitItem(itemIndex);
            this.path.add(itemIndex);

            while (!this.path.isEmpty()) {
                int currentPathSize = this.path.size();
                if (isPathIndexItem(currentPathSize - 1)) {
                    int currentItem = this.path.getInt(currentPathSize - 1);
                    for (int ingredientIndex = 0; ingredientIndex < this.ingredientCount; ingredientIndex++) {
                        if (!this.hasVisitedIngredient(ingredientIndex) &&
                                this.hasConnection(currentItem, ingredientIndex) &&
                                !this.isAssigned(currentItem, ingredientIndex)) {
                            this.visitIngredient(ingredientIndex);
                            this.path.add(ingredientIndex);
                            break;
                        }
                    }
                } else {
                    int currentIngredient = this.path.getInt(currentPathSize - 1);
                    if (!this.isSatisfied(currentIngredient)) {
                        return this.path;
                    }
                    for (int itemIndexCandidate = 0; itemIndexCandidate < this.itemCount; itemIndexCandidate++) {
                        if (!this.hasVisitedItem(itemIndexCandidate) &&
                                this.isAssigned(itemIndexCandidate, currentIngredient)) {
                            assert this.hasConnection(itemIndexCandidate, currentIngredient);

                            this.visitItem(itemIndexCandidate);
                            this.path.add(itemIndexCandidate);
                            break;
                        }
                    }
                }
                int newPathSize = this.path.size();
                if (newPathSize == currentPathSize) {
                    this.path.removeInt(newPathSize - 1);
                }
            }
            return null;
        }

        public boolean tryPick(int quantity) {
            int assignedIngredientsCount = 0;
            while (true) {
                IntList assignmentPath = this.tryAssigningNewItem(quantity);
                if (assignmentPath == null) {
                    boolean allIngredientsTried = assignedIngredientsCount == this.ingredientCount;
                    this.clearAllVisited();
                    this.clearSatisfied();

                    for (int ingredientIndex = 0; ingredientIndex < this.ingredientCount; ingredientIndex++) {
                        for (int itemIndex = 0; itemIndex < this.itemCount; itemIndex++) {
                            if (this.isAssigned(itemIndex, ingredientIndex)) {
                                this.unassign(itemIndex, ingredientIndex);
                                StackedContents.this.put(this.items.get(itemIndex), quantity);
                                break;
                            }
                        }
                    }

                    assert this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty();
                    return allIngredientsTried;
                }

                int firstItemIndex = assignmentPath.getInt(0);
                StackedContents.this.take(this.items.get(firstItemIndex), quantity);

                int lastIngredientIndex = assignmentPath.size() - 1;
                this.setSatisfied(assignmentPath.getInt(lastIngredientIndex));
                assignedIngredientsCount++;

                for (int pathIndex = 0; pathIndex < assignmentPath.size() - 1; pathIndex++) {
                    if (isPathIndexItem(pathIndex)) {
                        int itemIndex = assignmentPath.getInt(pathIndex);
                        int ingredientIndex = assignmentPath.getInt(pathIndex + 1);
                        this.assign(itemIndex, ingredientIndex);
                    } else {
                        int ingredientIndex = assignmentPath.getInt(pathIndex + 1);
                        int itemIndex = assignmentPath.getInt(pathIndex);
                        this.unassign(itemIndex, ingredientIndex);
                    }
                }
            }
        }

        private static boolean isPathIndexItem(int index) {
            return (index & 1) == 0;
        }

        private int visitedIngredientOffset() {
            return 0;
        }

        private int visitedIngredientCount() {
            return this.ingredientCount;
        }

        private int visitedItemOffset() {
            return this.visitedIngredientOffset() + this.visitedIngredientCount();
        }

        private int visitedItemCount() {
            return this.itemCount;
        }

        private int satisfiedOffset() {
            return this.visitedItemOffset() + this.visitedItemCount();
        }

        private int satisfiedCount() {
            return this.ingredientCount;
        }

        private int connectionOffset() {
            return this.satisfiedOffset() + this.satisfiedCount();
        }

        private int connectionCount() {
            return this.ingredientCount * this.itemCount;
        }

        private int residualOffset() {
            return this.connectionOffset() + this.connectionCount();
        }

        private int residualCount() {
            return this.ingredientCount * this.itemCount;
        }

        private boolean isSatisfied(int itemId) {
            return this.data.get(this.getSatisfiedIndex(itemId));
        }

        private void setSatisfied(int itemId) {
            this.data.set(this.getSatisfiedIndex(itemId));
        }

        private int getSatisfiedIndex(int itemId) {
            assert itemId >= 0 && itemId < this.ingredientCount;

            return this.satisfiedOffset() + itemId;
        }

        private void clearSatisfied() {
            this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
        }

        private void setConnection(int itemIndex, int ingredientIndex) {
            this.data.set(this.getConnectionIndex(itemIndex, ingredientIndex));
        }

        private boolean hasConnection(int itemIndex, int ingredientIndex) {
            return this.data.get(this.getConnectionIndex(itemIndex, ingredientIndex));
        }

        private int getConnectionIndex(int itemIndex, int ingredientIndex) {
            assert itemIndex >= 0 && itemIndex < this.itemCount;
            assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;
            return this.connectionOffset() + itemIndex * this.ingredientCount + ingredientIndex;
        }

        private boolean isAssigned(int itemIndex, int ingredientIndex) {
            return this.data.get(this.getResidualIndex(itemIndex, ingredientIndex));
        }

        private void assign(int itemIndex, int ingredientIndex) {
            int i = this.getResidualIndex(itemIndex, ingredientIndex);
            assert !this.data.get(i);
            this.data.set(i);
        }

        private void unassign(int itemIndex, int ingredientIndex) {
            int i = this.getResidualIndex(itemIndex, ingredientIndex);
            assert this.data.get(i);
            this.data.clear(i);
        }

        private int getResidualIndex(int itemIndex, int ingredientIndex) {
            assert itemIndex >= 0 && itemIndex < this.itemCount;
            assert ingredientIndex >= 0 && ingredientIndex < this.ingredientCount;
            return this.residualOffset() + itemIndex * this.ingredientCount + ingredientIndex;
        }

        private void visitIngredient(int index) {
            this.data.set(this.getVisitedIngredientIndex(index));
        }

        private boolean hasVisitedIngredient(int index) {
            return this.data.get(this.getVisitedIngredientIndex(index));
        }

        private int getVisitedIngredientIndex(int index) {
            assert index >= 0 && index < this.ingredientCount;
            return this.visitedIngredientOffset() + index;
        }

        private void visitItem(int index) {
            this.data.set(this.getVisitedItemIndex(index));
        }

        private boolean hasVisitedItem(int index) {
            return this.data.get(this.getVisitedItemIndex(index));
        }

        private int getVisitedItemIndex(int index) {
            assert index >= 0 && index < this.itemCount;
            return this.visitedItemOffset() + index;
        }

        private void clearAllVisited() {
            this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
            this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
        }

        private void clearRange(int start, int offset) {
            this.data.clear(start, start + offset);
        }
    }
}
