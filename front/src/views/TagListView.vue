<template>
  <div>
    <v-row class="mb-4" align="center">
      <v-col cols="auto">
        <v-btn color="primary" prepend-icon="mdi-plus" @click="openForm()">태그 추가</v-btn>
      </v-col>
    </v-row>

    <!-- 태그 목록 -->
    <v-card>
      <v-data-table :headers="headers" :items="tags" :loading="loading">
        <template #item.color="{ item }">
          <v-chip :color="item.color" size="small" variant="flat" class="text-white">
            {{ item.color }}
          </v-chip>
        </template>
        <template #item.createdAt="{ value }">
          {{ formatDate(value) }}
        </template>
        <template #item.actions="{ item }">
          <v-btn icon="mdi-pencil" size="small" variant="text" @click.stop="openForm(item)" />
          <v-btn icon="mdi-delete" size="small" variant="text" color="red" @click.stop="confirmDelete(item)" />
        </template>
      </v-data-table>
    </v-card>

    <!-- 추가/수정 다이얼로그 -->
    <v-dialog v-model="formDialog" max-width="450">
      <v-card>
        <v-card-title>{{ editing ? '태그 수정' : '태그 추가' }}</v-card-title>
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
        <v-card-actions>
          <v-spacer />
          <v-btn @click="formDialog = false">취소</v-btn>
          <v-btn color="primary" @click="save">저장</v-btn>
        </v-card-actions>
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

/** 테이블 헤더 */
const headers = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '이름', key: 'name' },
  { title: '색상', key: 'color', width: 120 },
  { title: '생성일', key: 'createdAt' },
  { title: '', key: 'actions', sortable: false, width: 100 },
]

/** 날짜 포맷 */
const formatDate = (v) => v ? v.replace('T', ' ').slice(0, 16) : '-'

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
