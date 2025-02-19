import { jsx as _jsx } from "react/jsx-runtime";
// src/components/tests/TaskBoard.test.tsx
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import TaskBoard from "../TaskBoard";
const mockDB = {
    tasks: {
        toArray: vi.fn(),
        update: vi.fn(),
    },
    transaction: vi.fn(),
};
// 测试数据
const mockTasks = [
    {
        id: 1,
        title: "Task 1",
        position: 0,
        completed: false,
        status: "todo",
        createdAt: new Date("2024-01-01"),
        dueDate: new Date("2024-03-20"),
    },
    {
        id: 2,
        title: "Task 2",
        position: 1,
        completed: true,
        status: "progress",
        createdAt: new Date("2024-01-02"),
        dueDate: new Date("2024-03-21"),
    },
];
beforeEach(() => {
    vi.clearAllMocks();
    // 初始化模拟实现
    mockDB.tasks.toArray.mockResolvedValue([...mockTasks]);
    mockDB.transaction.mockImplementation((_, __, callback) => callback());
});
describe("TaskBoard 组件测试", () => {
    it("正确处理拖拽排序", async () => {
        render(_jsx(TaskBoard, { db: mockDB }));
        await waitFor(() => screen.getByText("Task 1"));
        fireEvent.dragEnd(screen.getByText("Task 1").parentElement, {
            dataTransfer: { effectAllowed: "move" },
        });
        await waitFor(() => {
            expect(mockDB.transaction).toHaveBeenCalledWith("rw", mockDB.tasks, expect.any(Function));
            expect(mockDB.tasks.update).toHaveBeenCalledTimes(2);
        });
    });
    // 其他测试用例保持不变...
});
//# sourceMappingURL=TaskBoard.test.js.map