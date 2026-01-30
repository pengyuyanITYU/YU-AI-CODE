<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>

<template>
  <BasicLayout />
</template>

<style>
/* 全局样式优化 */
* {
  box-sizing: border-box;
}

html {
  overflow-x: hidden;
  background: #0f172a; /* 确保背景色一致，防止白屏 */
}

body {
  margin: 0;
  padding: 0;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell',
    'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  background: #0f172a;
  color: rgba(255, 255, 255, 0.95);
}

/* --- 强制覆盖 Ant Design 组件样式 (Dark Mode) --- */

/* Modal 弹窗 */
.ant-modal-content {
  background: linear-gradient(135deg, rgba(30, 41, 59, 1) 0%, rgba(15, 23, 42, 1) 100%) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7) !important;
}

.ant-modal-header {
  background: transparent !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15) !important;
}

.ant-modal-title {
  color: rgba(255, 255, 255, 0.98) !important;
}

.ant-modal-close {
  color: rgba(255, 255, 255, 0.6) !important;
}

.ant-modal-close:hover {
  color: rgba(255, 255, 255, 1) !important;
}

.ant-modal-body {
  color: rgba(255, 255, 255, 0.95) !important;
}

/* Popover / Tooltip / Dropdown */
.ant-popover-inner,
.ant-tooltip-inner,
.ant-dropdown-menu-submenu-popup .ant-dropdown-menu,
.ant-dropdown-menu {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.95) 100%) !important;
  color: rgba(255, 255, 255, 0.95) !important;
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5) !important;
}

.ant-popover-title {
  color: rgba(255, 255, 255, 0.98) !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15) !important;
}

.ant-popconfirm-title,
.ant-popconfirm-description {
  color: rgba(255, 255, 255, 0.95) !important;
}

.ant-popover-inner-content {
  color: rgba(255, 255, 255, 0.95) !important;
}

.ant-dropdown-menu-item {
  color: rgba(255, 255, 255, 0.95) !important;
}

.ant-dropdown-menu-item:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  color: rgba(255, 255, 255, 1) !important;
}

/* Input / Select / Form */
.ant-input,
.ant-select-selector {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(148, 163, 184, 0.2) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

.ant-input:hover,
.ant-select-selector:hover {
  border-color: rgba(59, 130, 246, 0.5) !important;
}

.ant-input:focus,
.ant-select-focused .ant-select-selector {
  border-color: rgba(59, 130, 246, 0.6) !important;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15) !important;
}

.ant-select-dropdown {
  background: #1e293b !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
}

.ant-select-item {
  color: rgba(255, 255, 255, 0.9) !important;
}

.ant-select-item-option-selected {
  background: rgba(59, 130, 246, 0.2) !important;
  color: white !important;
}

.ant-select-item-option-active {
  background: rgba(255, 255, 255, 0.1) !important;
}

.ant-select-arrow {
  color: rgba(148, 163, 184, 0.8) !important;
}

/* Message */
.ant-message-notice-content {
  background: #1e293b !important;
  color: rgba(255, 255, 255, 0.95) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
}

/* Pagination */
.ant-pagination-item {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(148, 163, 184, 0.2) !important;
}

.ant-pagination-item a {
  color: rgba(255, 255, 255, 0.9) !important;
}

.ant-pagination-item-active {
  background: rgba(59, 130, 246, 0.2) !important;
  border-color: rgba(59, 130, 246, 0.6) !important;
}

.ant-pagination-item-active a {
  color: rgba(59, 130, 246, 1) !important;
}

.ant-pagination-prev .ant-pagination-item-link,
.ant-pagination-next .ant-pagination-item-link {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(148, 163, 184, 0.2) !important;
  color: rgba(255, 255, 255, 0.6) !important;
}

.ant-pagination-options {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* Image Preview */
.ant-image-preview-root .ant-image-preview-mask {
  background-color: rgba(0, 0, 0, 0.85) !important;
}
.ant-image-preview-root .ant-image-preview-operations {
  background-color: rgba(0, 0, 0, 0.5) !important;
}

/* Card */
.ant-card {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.6) 0%, rgba(15, 23, 42, 0.7) 100%) !important;
  border-color: rgba(148, 163, 184, 0.15) !important;
}
.ant-card-head {
  border-bottom: 1px solid rgba(148, 163, 184, 0.15) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}
.ant-card-meta-title,
.ant-card-meta-description {
  color: rgba(255, 255, 255, 0.95) !important;
}

/* Spin / Loading */
.ant-spin-nested-loading > div > .ant-spin .ant-spin-dot {
  color: #3b82f6 !important;
}
.ant-spin-blur {
  opacity: 0.5 !important;
}
.ant-spin-blur:after {
  opacity: 0.5 !important;
}
.ant-spin-container:after {
  background: #0f172a !important;
}

/* Alert */
.ant-alert {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
}
.ant-alert-message {
  color: #fff !important;
}
.ant-alert-description {
  color: rgba(148, 163, 184, 0.9) !important;
}

/* Notification */
.ant-notification-notice {
  background: #1e293b !important;
  border: 1px solid rgba(148, 163, 184, 0.15) !important;
}
.ant-notification-notice-message {
  color: #fff !important;
}
.ant-notification-notice-description {
  color: rgba(255, 255, 255, 0.9) !important;
}
.ant-notification-notice-close {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* Drawer */
.ant-drawer-content {
  background: #0f172a !important;
}
.ant-drawer-header {
  background: transparent !important;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15) !important;
  color: #fff !important;
}
.ant-drawer-title {
  color: #fff !important;
}
.ant-drawer-close {
  color: rgba(255, 255, 255, 0.6) !important;
}
.ant-drawer-body {
  color: rgba(255, 255, 255, 0.95) !important;
}

/* Skeleton */
.ant-skeleton-content .ant-skeleton-title,
.ant-skeleton-content .ant-skeleton-paragraph > li {
  background: rgba(255, 255, 255, 0.1) !important;
}
.ant-skeleton-avatar {
  background: rgba(255, 255, 255, 0.1) !important;
}

/* Table */
.ant-table {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.95) !important;
}
.ant-table-thead > tr > th {
  background: rgba(30, 41, 59, 1) !important; /* darker header */
  border-bottom: 1px solid rgba(148, 163, 184, 0.15) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}
.ant-table-tbody > tr > td {
  border-bottom: 1px solid rgba(148, 163, 184, 0.1) !important;
  color: rgba(255, 255, 255, 0.9) !important;
}
.ant-table-tbody > tr:hover > td {
  background: rgba(255, 255, 255, 0.05) !important;
}

/* Empty */
.ant-empty-description {
  color: rgba(148, 163, 184, 0.7) !important;
}

/* Custom Scrollbar */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 4px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.5);
}

/* Layout */
.ant-layout {
  background: #0f172a !important;
}

/* Image */
.ant-image-placeholder {
  background: #1e293b !important;
}
</style>
