import { jsx as _jsx } from "react/jsx-runtime";
import { Chart as ChartJS, registerables, } from "chart.js";
import { Bar } from "react-chartjs-2";
ChartJS.register(...registerables);
const DataChart = ({ data, title }) => {
    const chartData = {
        labels: ["未完成", "已完成", "24小时内活跃"],
        datasets: [
            {
                label: "任务统计",
                data: [
                    data.filter((t) => !t.completed).length,
                    data.filter((t) => t.completed).length,
                    data.filter((t) => !t.completed &&
                        Date.now() - new Date(t.createdAt).getTime() < 86400000).length,
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
    const options = {
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
    return (_jsx("div", { className: "p-4 bg-white dark:bg-gray-800 rounded-lg shadow-md", children: _jsx(Bar, { data: chartData, options: options }) }));
};
export default DataChart;
//# sourceMappingURL=DataChart.js.map