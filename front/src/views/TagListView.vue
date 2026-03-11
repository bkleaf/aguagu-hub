<template>
  <div>
    <!-- 주요 태그 섹션 -->
    <v-card class="mb-4">
      <v-card-title class="d-flex align-center">
        <span>주요 태그</span>
        <v-spacer />
        <v-btn color="primary" size="small" prepend-icon="mdi-plus" variant="tonal" @click="openForm('MAIN')">추가</v-btn>
      </v-card-title>
      <v-card-text>
        <div v-if="mainTags.length === 0" class="text-grey text-body-2 pa-2">주요 태그가 없습니다.</div>
        <div class="d-flex flex-wrap ga-2">
          <v-chip
            v-for="tag in mainTags"
            :key="tag.id"
            :color="tag.color"
            variant="flat"
            class="text-white"
            closable
            @click="openForm('MAIN', tag)"
            @click:close.stop="confirmDelete(tag)"
          >
            {{ tag.name }}
          </v-chip>
        </div>
      </v-card-text>
    </v-card>

    <!-- 세부 태그 섹션 -->
    <v-card>
      <v-card-title class="d-flex align-center">
        <span>세부 태그</span>
        <v-spacer />
        <v-btn color="primary" size="small" prepend-icon="mdi-plus" variant="tonal" @click="openForm('DETAIL')">추가</v-btn>
      </v-card-title>
      <v-card-text>
        <div v-if="detailTags.length === 0" class="text-grey text-body-2 pa-2">세부 태그가 없습니다.</div>
        <div class="d-flex flex-wrap ga-2">
          <v-chip
            v-for="tag in detailTags"
            :key="tag.id"
            :color="tag.color"
            variant="outlined"
            closable
            @click="openForm('DETAIL', tag)"
            @click:close.stop="confirmDelete(tag)"
          >
            {{ tag.name }}
          </v-chip>
        </div>
      </v-card-text>
    </v-card>

    <!-- 추가/수정 다이얼로그 -->
    <v-dialog v-model="formDialog" max-width="500">
      <v-card>
        <v-card-title>{{ editing ? '태그 수정' : '태그 추가' }} ({{ formTagType === 'MAIN' ? '주요' : '세부' }})</v-card-title>
        <v-card-text>
          <v-form ref="formRef">
            <v-text-field
              ref="nameFieldRef"
              v-model="form.name"
              label="태그 이름"
              :rules="[v => !!v || '필수', v => (v && v.length <= 50) || '50자 이내']"
              class="mb-3"
              @keydown.enter="save"
            />
            <div class="text-subtitle-2 mb-2">색상 선택</div>
            <div class="d-flex flex-wrap ga-1 mb-3" style="max-height: 200px; overflow-y: auto;">
              <v-chip
                v-for="c in colorPresets"
                :key="c"
                :color="c"
                size="x-small"
                variant="flat"
                class="text-white"
                :class="{ 'border-md border-opacity-100': form.color === c }"
                style="cursor: pointer; min-width: 28px; height: 28px;"
                @click="form.color = c"
              >
                <v-icon v-if="form.color === c" size="x-small">mdi-check</v-icon>
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { fetchTags, createTag, updateTag, deleteTag } from '../api'

const loading = ref(false)
const tags = ref([])
const formDialog = ref(false)
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const editing = ref(false)
const editingId = ref(null)
const formTagType = ref('DETAIL')
const formRef = ref(null)
const nameFieldRef = ref(null)
const form = ref({ name: '', color: '#2196F3' })
const snackbar = ref({ show: false, text: '', color: '' })

/** HSL 기반 100개 색상 팔레트 생성 */
const colorPresets = (() => {
  const colors = []
  // 채도 80%, 밝기 45% 기준으로 색상환을 100등분
  for (let i = 0; i < 100; i++) {
    const hue = Math.round((i * 360) / 100)
    // HSL → hex 변환
    const s = 0.7
    const l = 0.45
    const c = (1 - Math.abs(2 * l - 1)) * s
    const x = c * (1 - Math.abs(((hue / 60) % 2) - 1))
    const m = l - c / 2
    let r, g, b
    if (hue < 60) { r = c; g = x; b = 0 }
    else if (hue < 120) { r = x; g = c; b = 0 }
    else if (hue < 180) { r = 0; g = c; b = x }
    else if (hue < 240) { r = 0; g = x; b = c }
    else if (hue < 300) { r = x; g = 0; b = c }
    else { r = c; g = 0; b = x }
    const toHex = (v) => Math.round((v + m) * 255).toString(16).padStart(2, '0')
    colors.push(`#${toHex(r)}${toHex(g)}${toHex(b)}`.toUpperCase())
  }
  return colors
})()

/** 랜덤 색상 선택 */
const getRandomColor = () => colorPresets[Math.floor(Math.random() * colorPresets.length)]

/** 주요 태그 목록 */
const mainTags = computed(() => tags.value.filter(t => t.tagType === 'MAIN'))

/** 세부 태그 목록 */
const detailTags = computed(() => tags.value.filter(t => t.tagType === 'DETAIL'))

/** 추가/수정 폼 열기 */
function openForm(tagType, item) {
  formTagType.value = tagType
  if (item) {
    editing.value = true
    editingId.value = item.id
    form.value = { name: item.name, color: item.color }
  } else {
    editing.value = false
    editingId.value = null
    form.value = { name: '', color: getRandomColor() }
  }
  formDialog.value = true
  /** 다이얼로그 렌더링 후 태그 이름 필드에 포커스 */
  nextTick(() => {
    nameFieldRef.value?.focus()
  })
}

/** 저장 */
async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  try {
    const payload = { name: form.value.name, color: form.value.color, tagType: formTagType.value }
    if (editing.value) {
      await updateTag(editingId.value, payload)
    } else {
      await createTag(payload)
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
