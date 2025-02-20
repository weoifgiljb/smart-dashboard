import React from "react";
import {
  Chart as ChartJS,
  ChartData,
  ChartOptions,
  registerables,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import { Task } from "@/types";
import styles from "../css/DataChart.module.scss";

ChartJS.register(...registerables);

interface ChartProps {
  data: Task[];
  title: string;
}

const DataChart: React.FC<ChartProps> = ({ data, title }) => {
  const chartData: ChartData<"bar"> = {
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

  const options: ChartOptions<"bar"> = {
    responsive: true,
    plugins: {
      title: {
        display: true,
        text: title,
        font: { size: 16 },
        color: document.documentElement.classList.contains('dark') ? '#e5e7eb' : '#333',
      },
      legend: {
        labels: {
          color: document.documentElement.classList.contains('dark') ? '#e5e7eb' : '#333',
        },
      },
    },
    scales: {
      x: {
        ticks: {
          color: document.documentElement.classList.contains('dark') ? '#e5e7eb' : '#333',
        },
        grid: {
          color: document.documentElement.classList.contains('dark') ? 'rgba(229, 231, 235, 0.1)' : 'rgba(51, 51, 51, 0.1)',
        },
      },
      y: {
        ticks: {
          color: document.documentElement.classList.contains('dark') ? '#e5e7eb' : '#333',
        },
        grid: {
          color: document.documentElement.classList.contains('dark') ? 'rgba(229, 231, 235, 0.1)' : 'rgba(51, 51, 51, 0.1)',
        },
      },
    },
  };

  return (
    <div className={styles.chartContainer}>
      <Bar data={chartData} options={options} />
    </div>
  );
};

export default DataChart;
