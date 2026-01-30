import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import AppCard from '../AppCard.vue'

const mockApp: API.AppVO = {
    id: 1,
    appName: '测试应用',
    cover: 'https://example.com/cover.jpg',
    deployKey: 'test-key',
    visualRange: true,
    user: {
        id: 1,
        userName: '测试用户',
        userAvatar: 'https://example.com/avatar.jpg',
    },
}

describe('AppCard', () => {
    it('应该渲染应用名称', () => {
        const wrapper = mount(AppCard, {
            props: { app: mockApp },
            global: {
                stubs: ['a-avatar', 'a-tag', 'a-button', 'a-space', 'a-switch', 'a-popconfirm'],
            },
        })
        expect(wrapper.text()).toContain('测试应用')
    })

    it('非精选卡片应该显示删除按钮', () => {
        const wrapper = mount(AppCard, {
            props: { app: mockApp, featured: false },
            global: {
                stubs: ['a-avatar', 'a-tag', 'a-button', 'a-space', 'a-switch', 'a-popconfirm'],
            },
        })
        expect(wrapper.find('.app-info-actions').exists()).toBe(true)
    })

    it('精选卡片不应该显示删除按钮', () => {
        const wrapper = mount(AppCard, {
            props: { app: mockApp, featured: true },
            global: {
                stubs: ['a-avatar', 'a-tag', 'a-button', 'a-space', 'a-switch', 'a-popconfirm'],
            },
        })
        expect(wrapper.find('.app-info-actions').exists()).toBe(false)
    })

    it('应该触发view-chat事件', async () => {
        const wrapper = mount(AppCard, {
            props: { app: mockApp },
            global: {
                stubs: ['a-avatar', 'a-tag', 'a-button', 'a-space', 'a-switch', 'a-popconfirm'],
            },
        })
        wrapper.vm.$emit('view-chat', mockApp.id)
        expect(wrapper.emitted('view-chat')).toBeTruthy()
        expect(wrapper.emitted('view-chat')![0]).toEqual([1])
    })

    it('应该正确emit delete-app事件', () => {
        const wrapper = mount(AppCard, {
            props: { app: mockApp },
            global: {
                stubs: ['a-avatar', 'a-tag', 'a-button', 'a-space', 'a-switch', 'a-popconfirm'],
            },
        })
        wrapper.vm.$emit('delete-app', mockApp)
        expect(wrapper.emitted('delete-app')).toBeTruthy()
        expect(wrapper.emitted('delete-app')![0]).toEqual([mockApp])
    })
})
