import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Component } from "react";
export default class ErrorBoundary extends Component {
    state = {
        hasError: false,
    };
    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }
    componentDidCatch(error, errorInfo) {
        console.error("Error Boundary Catch:", error, errorInfo);
        this.setState({ errorInfo });
    }
    handleReset = () => {
        this.setState({ hasError: false, error: undefined, errorInfo: undefined });
    };
    render() {
        if (this.state.hasError) {
            return (this.props.fallback || (_jsx("div", { className: "p-6 bg-red-50 dark:bg-red-900 text-red-700 dark:text-red-200 rounded-lg", children: _jsxs("div", { className: "max-w-4xl mx-auto", children: [_jsxs("h2", { className: "text-2xl font-bold mb-4 flex items-center", children: [_jsx("svg", { className: "w-8 h-8 mr-2", fill: "none", stroke: "currentColor", viewBox: "0 0 24 24", xmlns: "http://www.w3.org/2000/svg", children: _jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" }) }), "Something went wrong!"] }), this.state.error && (_jsxs("div", { className: "mb-4", children: [_jsx("p", { className: "font-mono text-sm mb-2", children: this.state.error.toString() }), this.state.errorInfo?.componentStack && (_jsx("pre", { className: "text-xs bg-black bg-opacity-20 p-3 rounded overflow-auto", children: this.state.errorInfo.componentStack }))] })), _jsx("button", { onClick: this.handleReset, className: "px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition-colors", children: "Try Again" })] }) })));
        }
        return this.props.children;
    }
}
//# sourceMappingURL=ErrorBoundary.js.map