package com.jamieswhiteshirt.clotheslinefabric.common.util;

import com.jamieswhiteshirt.clotheslinefabric.api.NetworkState;
import com.jamieswhiteshirt.clotheslinefabric.api.Path;
import com.jamieswhiteshirt.clotheslinefabric.api.Tree;
import com.jamieswhiteshirt.clotheslinefabric.common.impl.NetworkStateImpl;
import com.jamieswhiteshirt.clotheslinefabric.api.util.MutableSortedIntMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class NetworkStateBuilder {
    public static final class SplitResult {
        private final NetworkStateBuilder state;
        private final List<NetworkStateBuilder> subStates;

        public SplitResult(NetworkStateBuilder state, List<NetworkStateBuilder> subStates) {
            this.state = state;
            this.subStates = subStates;
        }

        public NetworkStateBuilder getState() {
            return state;
        }

        public List<NetworkStateBuilder> getSubStates() {
            return subStates;
        }
    }

    public static NetworkStateBuilder fromAbsolute(NetworkState state) {
        MutableSortedIntMap<ItemStack> attachments = state.getAttachments();
        MutableSortedIntMap<ItemStack> itemStacks;
        if (state.getPathLength() != 0) {
            int midAttachmentKey = state.offsetToAttachmentKey(0);
            itemStacks = MutableSortedIntMap.concatenate(Arrays.asList(
                attachments.shiftedSubMap(midAttachmentKey, attachments.getMaxKey()),
                attachments.shiftedSubMap(0, midAttachmentKey)
            ));
        } else {
            itemStacks = state.getAttachments();
        }
        TreeBuilder tree = TreeBuilder.fromAbsolute(state.getTree(), itemStacks, state.getShift());
        return new NetworkStateBuilder(state.getMomentum(), tree);
    }

    public static NetworkStateBuilder emptyRoot(int momentum, BlockPos root) {
        return new NetworkStateBuilder(momentum, TreeBuilder.emptyRoot(root));
    }

    private int momentum;
    private TreeBuilder treeRoot;

    private NetworkStateBuilder(int momentum, TreeBuilder treeRoot) {
        this.momentum = momentum;
        this.treeRoot = treeRoot;
    }

    public void reroot(BlockPos pos) {
        treeRoot = treeRoot.reroot(pos);
    }

    public void addEdge(BlockPos fromPos, BlockPos toPos) {
        treeRoot.addChild(fromPos, TreeBuilder.emptyRoot(toPos));
        momentum /= 2;
    }

    public void addSubState(BlockPos fromPos, NetworkStateBuilder other) {
        treeRoot.addChild(fromPos, other.treeRoot);
        momentum = (momentum + other.momentum) / 2;
    }

    public SplitResult splitRoot() {
        TreeBuilder.SplitResult result = treeRoot.splitNode();
        return new SplitResult(
            new NetworkStateBuilder(momentum, result.getTree()),
            result.getSubTrees().stream()
                .map(tree -> new NetworkStateBuilder(momentum, tree))
                .collect(Collectors.toList())
        );
    }

    public SplitResult splitEdge(BlockPos pos) {
        TreeBuilder.SplitResult result = treeRoot.splitEdge(pos);
        return new SplitResult(
            new NetworkStateBuilder(momentum, result.getTree()),
            result.getSubTrees().stream()
                .map(tree -> new NetworkStateBuilder(momentum, tree))
                .collect(Collectors.toList())
        );
    }

    public NetworkState build() {
        LinkedList<MutableSortedIntMap<ItemStack>> attachmentsList = new LinkedList<>();
        Tree tree = treeRoot.build(attachmentsList, 0);
        Path path = PathBuilder.buildPath(tree);
        LongSet chunkSpan = ChunkSpan.ofPath(path);
        return new NetworkStateImpl(0, 0, momentum, momentum, tree, path, chunkSpan, MutableSortedIntMap.concatenate(attachmentsList));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkStateBuilder that = (NetworkStateBuilder) o;
        return momentum == that.momentum &&
            Objects.equals(treeRoot, that.treeRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(momentum, treeRoot);
    }
}
