// src/components/tests/TaskBoard.test.tsx
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import TaskBoard from "../TaskBoard";
import type { DashboardDB } from "../../services/db";
import type { Task, TaskStatus } from "../../types";
import {
  DragDropContext,
  Droppable,
  Draggable,
  type DropResult,
} from "@hello-pangea/dnd";

// 使用更兼容的模拟方式
type MockDB = {
  tasks: {
    toArray: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  transaction: ReturnType<typeof vi.fn>;
};

const mockDB: MockDB & DashboardDB = {
  tasks: {
    toArray: vi.fn(),
    update: vi.fn(),
  },
  transaction: vi.fn(),
} as any;

// 测试数据
const mockTasks: Task[] = [
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
  (mockDB.tasks.toArray as any).mockResolvedValue([...mockTasks]);
  (mockDB.transaction as any).mockImplementation(
    (_: any, __: any, callback: () => Promise<void>) => callback()
  );
});

describe("TaskBoard 组件测试", () => {
  it("正确处理拖拽排序", async () => {
    render(<TaskBoard db={mockDB} />);
    await waitFor(() => screen.getByText("Task 1"));

    fireEvent.dragEnd(screen.getByText("Task 1").parentElement!, {
      dataTransfer: { effectAllowed: "move" },
    });

    await waitFor(() => {
      expect(mockDB.transaction).toHaveBeenCalledWith(
        "rw",
        mockDB.tasks,
        expect.any(Function)
      );
      expect(mockDB.tasks.update).toHaveBeenCalledTimes(2);
    });
  });

  // 其他测试用例保持不变...
});
