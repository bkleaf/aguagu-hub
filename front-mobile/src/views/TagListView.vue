<template>
  <div>
    <!-- 태그 추가 버튼 -->
    <v-btn color="primary" block prepend-icon="mdi-plus" class="mb-3" @click="openForm()">태그 추가</v-btn>

    <!-- 로딩 -->
    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-2" />

    <!-- 태그 카드 리스트 -->
    <v-card
      v-for="tag in tags"
      :key="tag.id"
      class="mb-2"
      variant="outlined"
      @click="openForm(tag)"
    >
      <v-card-text class="py-2">
        <div class="d-flex justify-space-between align-center">
          <div class="d-flex align-center">
            <v-chip :color="tag.color" size="small" variant="flat" class="text-white mr-2">
              {{ tag.name }}
            </v-chip>
            <span class="text-caption text-grey">{{ tag.color }}</span>
          </div>
          <v-btn icon="mdi-delete" size="small" variant="text" color="red" @click.stop="confirmDelete(tag)" />
        </div>
      </v-card-text>
    </v-card>

    <v-card v-if="!loading && tags.length === 0" variant="outlined">
      <v-card-text class="text-center text-grey py-6">등록된 태그가 없습니다</v-card-text>
    </v-card>

    <!-- 추가/수정 다이얼로그 (풀스크린) -->
    <v-dialog v-model="formDialog" fullscreen transition="dialog-bottom-transition">
      <v-card>
        <v-toolbar color="primary" density="compact">
          <v-btn icon="mdi-close" @click="formDialog = false" />
          <v-toolbar-title class="text-body-1">{{ editing ? '태그 수정' : '태그 추가' }}</v-toolbar-title>
          <v-spacer />
          <v-btn variant="text" @click="save">저장</v-btn>
        </v-toolbar>
        <v-card-text>
          <v-form ref="formRef">
            <v-text-field
              v-model="form.name"
              label="태그 이름"
              :rules="[v => !!v || '필수', v => (v && v.length <= 50) || '50자 이내']"
              class="mb-3"
            />
            <div class="text-subtitle-2 mb-2">색상 선택</div>
            <div class="d-flex flex-wrap ga-2 mb-3">
              <v-chip
                v-for="c in colorPresets"
                :key="c"
                :color="c"
                size="small"
                variant="flat"
                class="text-white"
                :class="{ 'border-md border-opacity-100': form.color === c }"
                style="cursor: pointer"
                @click="form.color = c"
              >
                <v-icon v-if="form.color === c" size="small">mdi-check</v-icon>
                <span v-else>&nbsp;&nbsp;</span>
              </v-chip>
            </div>
            <v-text-field
              v-model="form.color"
              label="색상 코드 (hex)"
              :rules="[v => /^#[0-9A-Fa-f]{6}$/.test(v) || '올바른 hex 색상 형식 (예: #2196F3)']"
              density="compact"
            >
              <template #prepend>
                <div :style="{ width: '24px', height: '24px', borderRadius: '4px', backgroundColor: form.color }" />
              </template>
            </v-text-field>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- 삭제 확인 다이얼로그 -->
    <v-dialog v-model="deleteDialog" max-width="350">
      <v-card>
        <v-card-title>태그 삭제</v-card-title>
        <v-card-text>
          "{{ deleteTarget?.name }}" 태그를 삭제하시겠습니까?
          이 태그가 할당된 거래에서도 해제됩니다.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="deleteDialog = false">취소</v-btn>
          <v-btn color="red" @click="doDelete">삭제</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color" timeout="3000">
      {{ snackbar.text }}
    </v-snackbar>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchTags, createTag, updateTag, deleteTag } from '../api'

const loading = ref(false)
const tags = ref([])
const formDialog = ref(false)
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = ref({ name: '', color: '#2196F3' })
const snackbar = ref({ show: false, text: '', color: '' })

/** 색상 프리셋 */
const colorPresets = [
  '#F44336', '#E91E63', '#9C27B0', '#673AB7',
  '#3F51B5', '#2196F3', '#03A9F4', '#00BCD4',
  '#009688', '#4CAF50', '#8BC34A', '#CDDC39',
  '#FFC107', '#FF9800', '#FF5722', '#795548',
]

/** 추가/수정 폼 열기 */
function openForm(item) {
  if (item) {
    editing.value = true
    editingId.value = item.id
    form.value = { name: item.name, color: item.color }
  } else {
    editing.value = false
    editingId.value = null
    form.value = { name: '', color: '#2196F3' }
  }
  formDialog.value = true
}

/** 저장 */
async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  try {
    if (editing.value) {
      await updateTag(editingId.value, form.value)
    } else {
      await createTag(form.value)
    }
    snackbar.value = { show: true, text: '저장 완료', color: 'green' }
    formDialog.value = false
    load()
  } catch (e) {
    snackbar.value = { show: true, text: '저장 실패: ' + (e.response?.data?.message || e.message), color: 'red' }
  }
}

/** 삭제 확인 */
function confirmDelete(item) {
  deleteTarget.value = item
  deleteDialog.value = true
}

/** 삭제 실행 */
async function doDelete() {
  try {
    await deleteTag(deleteTarget.value.id)
    snackbar.value = { show: true, text: '삭제 완료', color: 'green' }
    deleteDialog.value = false
    load()
  } catch (e) {
    snackbar.value = { show: true, text: '삭제 실패', color: 'red' }
  }
}

/** 목록 로드 */
async function load() {
  loading.value = true
  try {
    const res = await fetchTags()
    tags.value = res.data
  } catch (e) {
    console.error('태그 목록 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
