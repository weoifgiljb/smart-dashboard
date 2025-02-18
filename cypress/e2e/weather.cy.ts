// cypress/e2e/weather.cy.ts

/// <reference types="cypress" />

// 将全局扩展移到类型声明文件中
describe("天气组件端到端测试", () => {
  beforeEach(() => {
    cy.intercept("GET", "**/openweathermap.org/**", {
      fixture: "weather.json",
    }).as("getWeather");

    cy.visit("/");
  });

  it("正确显示天气卡片", () => {
    cy.get(".weather-card").as("card");

    cy.get("@card").should("be.visible");
    cy.get("@card").contains("上海").should("exist");
    cy.get("@card").contains("25°C").should("exist");
    cy.get("@card").find('img[alt="晴天"]').should("exist");
  });

  it("显示错误状态", () => {
    cy.intercept("GET", "**/openweathermap.org/**", {
      statusCode: 500,
      body: "API Error",
    });

    cy.reload();
    cy.contains("⚠️ 天气请求失败: 500").should("be.visible");
  });
});
