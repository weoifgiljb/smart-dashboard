import type { Layout } from "react-grid-layout";

export interface DashboardState {
  theme: "light" | "dark";
  layout: Layout[];
  lastUpdated: Date;
}

export interface Task {
  id?: number;
  title: string;
  completed: boolean;
  dueDate?: Date;
  priority: "low" | "medium" | "high";
  description?: string;
  createdAt: Date;
  updatedAt: Date;
  status: "todo" | "progress" | "done";
  position: number;
}

export interface WeatherData {
  temperature: number;
  condition: string;
  humidity: number;
  windSpeed: number;
  location: string;
  lastUpdated: Date;
}