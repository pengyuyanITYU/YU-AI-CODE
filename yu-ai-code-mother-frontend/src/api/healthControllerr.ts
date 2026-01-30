import { useRouter } from "vue-router";
import type { MenuProps } from 'ant-design-vue'
const router = useRouter();

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}
