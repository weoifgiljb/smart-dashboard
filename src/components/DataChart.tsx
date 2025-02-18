import React from "react";
import {
  Chart as ChartJS,
  ChartData,
  ChartOptions,
  registerables,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import { Task } from "../services/db";

ChartJS.register(...registerables);

interface ChartProps {
  data: Task[];
  title: string;
}

const DataChart: React.FC<ChartProps> = ({ data, title }) => {
  const chartData: ChartData = {
    labels: ["未完成", "已完成", "24小时内活跃"],
    datasets: [
      {
        label: "任务统计",
        data: [
          data.filter((t) => !t.completed).length,
          data.filter((t) => t.completed).length,
          data.filter(
            (t) =>
              !t.completed &&
              Date.now() - new Date(t.createdAt).getTime() < 86400000
          ).length,
        ],
        backgroundColor: [
          "rgba(255, 99, 132, 0.5)",
          "rgba(75, 192, 192, 0.5)",
          "rgba(54, 162, 235, 0.5)",
        ],
        borderColor: [
          "rgb(255, 99, 132)",
          "rgb(75, 192, 192)",
          "rgb(54, 162, 235)",
        ],
        borderWidth: 1,
      },
    ],
  };

  const options: ChartOptions = {
    responsive: true,
    plugins: {
      title: {
        display: true,
        text: title,
        font: { size: 16 },
        color: "#333",
      },
    },
  };

  return (
    <div className="p-4 bg-white dark:bg-gray-800 rounded-lg shadow-md">
      <Bar data={chartData} options={options} />
    </div>
  );
};

export default DataChart;
