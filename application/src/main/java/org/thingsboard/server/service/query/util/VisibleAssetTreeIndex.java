/** 文件职责：将中性资产树索引为告警分析所需的父节点和末级资产关系。 */
package org.thingsboard.server.service.query.util;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.service.query.VisibleAssetTree;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 一次遍历资产树，构建设备直属父资产、末级父资产和绝对路径索引。
 * 该类放在 query 公共包，避免 alarms 依赖 monitoring 的专有 DTO。
 */
public final class VisibleAssetTreeIndex {

    private final Map<UUID, String> parentNameByDevice;
    private final Map<UUID, LeafAsset> leafAssetsById;
    private final Map<UUID, UUID> parentAssetByDevice;

    private VisibleAssetTreeIndex(Map<UUID, String> parentNameByDevice,
                                 Map<UUID, LeafAsset> leafAssetsById,
                                 Map<UUID, UUID> parentAssetByDevice) {
        this.parentNameByDevice = Collections.unmodifiableMap(parentNameByDevice);
        this.leafAssetsById = Collections.unmodifiableMap(leafAssetsById);
        this.parentAssetByDevice = Collections.unmodifiableMap(parentAssetByDevice);
    }

    /** 从可见资产树创建索引。 */
    public static VisibleAssetTreeIndex build(VisibleAssetTree tree) {
        Map<UUID, String> parentNames = new LinkedHashMap<>();
        Map<UUID, LeafAsset> leafAssets = new LinkedHashMap<>();
        Map<UUID, UUID> parentByDevice = new LinkedHashMap<>();
        if (tree == null || tree.roots() == null) {
            return new VisibleAssetTreeIndex(parentNames, leafAssets, parentByDevice);
        }
        for (VisibleAssetTree.Node root : tree.roots()) {
            indexNode(root, root == null ? "" : root.name(), parentNames, leafAssets, parentByDevice);
        }
        return new VisibleAssetTreeIndex(parentNames, leafAssets, parentByDevice);
    }

    public Map<UUID, String> getParentNameByDevice() {
        return parentNameByDevice;
    }

    public Map<UUID, LeafAsset> getLeafAssetsById() {
        return leafAssetsById;
    }

    public Map<UUID, UUID> getParentAssetByDevice() {
        return parentAssetByDevice;
    }

    private static void indexNode(VisibleAssetTree.Node node, String currentPath,
                                  Map<UUID, String> parentNames,
                                  Map<UUID, LeafAsset> leafAssets,
                                  Map<UUID, UUID> parentByDevice) {
        if (node == null || node.id() == null) {
            return;
        }
        if (node.entityType() == EntityType.ASSET) {
            boolean hasChildren = node.children() != null && !node.children().isEmpty();
            boolean directChildrenAreDevices = hasChildren;
            if (node.children() != null) {
                for (VisibleAssetTree.Node child : node.children()) {
                    if (child == null || child.entityType() != EntityType.DEVICE) {
                        directChildrenAreDevices = false;
                    }
                    if (child != null && child.entityType() == EntityType.DEVICE && child.id() != null) {
                        parentNames.put(child.id(), node.name());
                    }
                }
            }
            if (directChildrenAreDevices) {
                leafAssets.put(node.id(), new LeafAsset(node.id(), node.name(), currentPath));
                for (VisibleAssetTree.Node child : node.children()) {
                    if (child != null && child.entityType() == EntityType.DEVICE && child.id() != null) {
                        parentByDevice.put(child.id(), node.id());
                    }
                }
            }
        }
        if (node.children() == null) {
            return;
        }
        for (VisibleAssetTree.Node child : node.children()) {
            if (child != null && child.entityType() == EntityType.ASSET) {
                indexNode(child, appendPath(currentPath, child.name()),
                        parentNames, leafAssets, parentByDevice);
            }
        }
    }

    public record LeafAsset(UUID id, String name, String path) {
    }

    private static String appendPath(String parentPath, String childName) {
        if (parentPath == null || parentPath.isBlank()) {
            return childName == null ? "" : childName;
        }
        if (childName == null || childName.isBlank()) {
            return parentPath;
        }
        return parentPath + "/" + childName;
    }
}
