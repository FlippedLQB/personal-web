<template>
  <n-tree
    :data="treeData"
    :checked-keys="checkedKeys"
    :default-checked-keys="checkedKeys"
    cascade
    checkable
    :selectable="false"
    key-field="id"
    label-field="permName"
    children-field="children"
    @update:checked-keys="handleCheck"
  />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 权限树原始数据
  data: { type: Array, default: () => [] },
  // 已选中的权限 ID 数组
  modelValue: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue'])

const treeData = computed(() => props.data || [])
const checkedKeys = computed(() => props.modelValue || [])

function handleCheck(keys) {
  emit('update:modelValue', keys)
}
</script>
