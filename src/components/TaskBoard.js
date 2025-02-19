import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
// src/components/TaskBoard.tsx 最终修正版
import { useEffect, useState } from "react";
import { DragDropContext, Droppable, Draggable, } from "@hello-pangea/dnd";
export default function TaskBoard({ db }) {
    const [tasks, setTasks] = useState([]);
    useEffect(() => {
        const loadTasks = async () => {
            const allTasks = await db.tasks.toArray();
            setTasks(allTasks.sort((a, b) => a.position - b.position));
        };
        loadTasks();
    }, [db]);
    const handleDragEnd = async (result) => {
        if (!result.destination)
            return;
        // 创建新任务数组
        const newTasks = [...tasks];
        const [movedTask] = newTasks.splice(result.source.index, 1);
        newTasks.splice(result.destination.index, 0, movedTask);
        // 验证有效的status值
        const validStatuses = ["todo", "progress", "done"];
        const newStatus = validStatuses.includes(result.destination.droppableId)
            ? result.destination.droppableId
            : movedTask.status;
        // 生成更新后的任务列表
        const updatedTasks = newTasks.map((task, index) => ({
            ...task,
            position: index,
            status: task.id === movedTask.id ? newStatus : task.status,
        }));
        setTasks(updatedTasks);
        try {
            // 确保事务执行
            await db.transaction("rw", db.tasks, async () => {
                // 批量更新所有需要修改的任务
                await Promise.all(updatedTasks.map((task) => {
                    // 仅更新需要修改的字段
                    const updates = { position: task.position };
                    if (task.id === movedTask.id) {
                        updates.status = newStatus;
                    }
                    return db.tasks.update(task.id, updates);
                }));
            });
        }
        catch (error) {
            console.error("更新任务失败:", error);
            // 回滚时需要重新获取数据
            const originalTasks = await db.tasks.toArray();
            setTasks(originalTasks.sort((a, b) => a.position - b.position));
        }
    };
    return (_jsx(DragDropContext, { onDragEnd: handleDragEnd, children: _jsx(Droppable, { droppableId: "todo", children: (provided) => (_jsxs("div", { ...provided.droppableProps, ref: provided.innerRef, className: "space-y-2", children: [tasks.map((task, index) => (_jsx(Draggable, { draggableId: String(task.id), index: index, children: (provided) => (_jsx("div", { ref: provided.innerRef, ...provided.draggableProps, ...provided.dragHandleProps, className: "p-3 bg-gray-100 dark:bg-gray-700 rounded-lg shadow-sm transition-transform duration-75 hover:scale-[1.02]", style: provided.draggableProps.style })) }, task.id))), provided.placeholder] })) }) }));
}
//# sourceMappingURL=TaskBoard.js.map