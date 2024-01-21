package cn.apisium.papershelled.plugin;

import java.util.Collections;
import java.util.List;

public final class PaperShelledDescription {

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    private List<String> mixins;

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    private List<String> additions;

    public List<String> getMixins() {
        return mixins == null ? Collections.emptyList() : Collections.unmodifiableList(mixins);
    }

    public List<String> getAdditions() {
        return additions == null ? Collections.emptyList() : Collections.unmodifiableList(additions);
    }
}
