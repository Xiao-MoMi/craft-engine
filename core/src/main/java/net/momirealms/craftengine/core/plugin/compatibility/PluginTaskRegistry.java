package net.momirealms.craftengine.core.plugin.compatibility;

import java.util.HashMap;
import java.util.Map;

public final class PluginTaskRegistry {
    private static class Node {
        final PluginTask task;
        Node prev;
        Node next;

        Node(PluginTask task) {
            this.task = task;
        }
    }

    private final Node head = new Node(null); // 哨兵头节点
    private final Node tail = new Node(null); // 哨兵尾节点
    private final Map<String, Node> pluginNodeMap = new HashMap<>();

    public PluginTaskRegistry() {
        head.next = tail;
        tail.prev = head;
    }

    public void registerTask(PluginTask task) {
        PluginTask.Priority priority = task.priority();
        Node newNode = new Node(task);
        String pluginName = task.plugin();

        if (this.pluginNodeMap.containsKey(pluginName)) {
            throw new IllegalArgumentException("Duplicate task for plugin: " + pluginName);
        }

        switch (priority.position()) {
            case HEAD:
                insertAfter(this.head, newNode);
                break;
            case TAIL:
                insertBefore(this.tail, newNode);
                break;
            case BEFORE_PLUGIN:
                Node targetBefore = this.pluginNodeMap.get(priority.relativePlugin());
                if (targetBefore == null) {
                    throw new IllegalArgumentException("Target plugin not found: " + priority.relativePlugin());
                }
                insertBefore(targetBefore, newNode);
                break;
            case AFTER_PLUGIN:
                Node targetAfter =this. pluginNodeMap.get(priority.relativePlugin());
                if (targetAfter == null) {
                    throw new IllegalArgumentException("Target plugin not found: " + priority.relativePlugin());
                }
                insertAfter(targetAfter, newNode);
                break;
        }

        this.pluginNodeMap.put(pluginName, newNode);
    }

    private void insertAfter(Node existing, Node newNode) {
        newNode.next = existing.next;
        newNode.prev = existing;
        existing.next.prev = newNode;
        existing.next = newNode;
    }

    private void insertBefore(Node existing, Node newNode) {
        newNode.prev = existing.prev;
        newNode.next = existing;
        existing.prev.next = newNode;
        existing.prev = newNode;
    }

    public void executeTasks() {
        try {
            Node current = head.next;
            while (current != tail) {
                current.task.task().run();
                current = current.next;
            }
        } catch (Throwable ignored) {
            // 不要管其他插件的异常，应该他们自己处理
        }
    }

    public String getExecutionOrder() {
        StringBuilder sb = new StringBuilder();
        Node current = head.next;
        while (current != tail) {
            sb.append(current.task.plugin());
            if (current.next != tail) sb.append(" -> ");
            current = current.next;
        }
        return sb.toString();
    }
}