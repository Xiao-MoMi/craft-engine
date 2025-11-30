package net.momirealms.craftengine.core.util.snbt.parse;

public interface Control {
    Control UNBOUND = new Control() {
        @Override
        public void cut() {
        }

        @Override
        public boolean hasCut() {
            return false;
        }
    };

    void cut();

    boolean hasCut();
}
