<template>
  <div>
    <!-- 한도 추가 버튼 -->
    <v-btn color="primary" prepend-icon="mdi-plus" block class="mb-3" @click="openForm()">한도 추가</v-btn>

    <!-- 로딩 -->
    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-2" />

    <!-- 한도 카드 리스트 -->
    <v-card
      v-for="limit in limits"
      :key="limit.id"
      class="mb-3"
      variant="outlined"
      @click="toggleExpand(limit.id)"
    >
      <v-card-text class="pb-1">
        <div class="d-flex justify-space-between align-center mb-1">
          <div>
            <span class="text-body-2 font-weight-medium">{{ limit.cardCompanyName }}</span>
            <span class="text-caption text-grey ml-1">{{ limit.cardLastFourDigits }}</span>
          </div>
          <v-chip size="x-small" :color="limitTypeColor(limit.limitTypeName)">{{ limit.limitTypeName }}</v-chip>
        </div>

        <!-- 프로그레스 바 (사용액 / 한도) -->
        <v-progress-linear
          :model-value="usagePercent(limit)"
          :color="usageColor(limit)"
          height="8"
          rounded
          class="mb-1"
        />
        <div class="d-flex justify-space-between text-caption">
          <span>{{ formatAmount(limit.usedAmount) }} 사용</span>
          <span>{{ formatAmount(limit.remaining) }} 잔여</span>
        </div>
      </v-card-text>

      <!-- 확장 상세 정보 -->
      <v-expand-transition>
        <div v-if="expandedId === limit.id">
          <v-divider />
          <v-card-text class="pt-2">
            <v-list density="compact" class="pa-0">
              <v-list-item title="한도 금액" :subtitle="formatAmount(limit.limitAmount)" />
              <v-list-item v-if="limit.limitType === 'CUSTOM' && limit.customStartDate" title="기간" :subtitle="`${limit.customStartDate} ~ ${limit.customEndDate}`" />
            </v-list>
            <div class="d-flex justify-end mt-2">
              <v-btn size="small" variant="text" icon="mdi-pencil" @click.stop="openForm(limit)" />
              <v-btn size="small" variant="text" icon="mdi-delete" color="red" @click.stop="confirmDelete(limit)" />
            </div>
          </v-card-text>
        </div>
      </v-expand-transition>
    </v-card>

    <v-card v-if="!loading && limits.length === 0" variant="outlined">
      <v-card-text class="text-center text-grey py-6">등록된 한도가 없습니다</v-card-text>
    </v-card>

    <!-- 추가/수정 다이얼로그 (풀스크린) -->
    <v-dialog v-model="formDialog" fullscreen transition="dialog-bottom-transition">
      <v-card>
        <v-toolbar color="primary" density="compact">
          <v-btn icon="mdi-close" @click="formDialog = false" />
          <v-toolbar-title class="text-body-1">{{ editing ? '한도 수정' : '한도 추가' }}</v-toolbar-title>
          <v-spacer />
          <v-btn variant="text" @click="saveLimit">저장</v-btn>
        </v-toolbar>
        <v-card-text>
          <v-form ref="formRef">
            <v-select
              v-model="form.creditCardId"
              :items="creditCardItems"
              label="신용카드"
              :rules="[v => !!v || '필수']"
              :disabled="editing"
            />
            <v-select
              v-model="form.limitType"
              :items="limitTypeItems"
              label="한도 유형"
              :rules="[v => !!v || '필수']"
            />
            <v-text-field
              v-model.number="form.limitAmount"
              label="한도 금액 (원)"
              type="number"
              :rules="[v => v > 0 || '양수여야 합니다']"
            />
            <template v-if="form.limitType === 'CUSTOM'">
              <v-text-field
                v-model="form.customStartDate"
                label="시작일"
                type="date"
                :rules="[v => !!v || '커스텀 유형은 시작일 필수']"
              />
              <v-text-field
                v-model="form.customEndDate"
                label="종료일"
                type="date"
                :rules="[v => !!v || '커스텀 유형은 종료일 필수']"
              />
            </template>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- 삭제 확인 다이얼로그 -->
    <v-dialog v-model="deleteDialog" max-width="320">
      <v-card>
        <v-card-title class="text-body-1">한도 삭제</v-card-title>
        <v-card-text>{{ deleteTarget?.cardCompanyName }} ({{ deleteTarget?.cardLastFourDigits }}) 한도를 삭제하시겠습니까?</v-card-text>
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
import { fetchLimits, createOrUpdateLimit, deleteLimit, fetchCreditCards } from '../api'

const loading = ref(false)
const limits = ref([])
const creditCardItems = ref([])
const expandedId = ref(null)
const formDialog = ref(false)
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const editing = ref(false)
const formRef = ref(null)
const form = ref({ creditCardId: null, limitType: 'MONTHLY', limitAmount: 0, customStartDate: null, customEndDate: null })
const snackbar = ref({ show: false, text: '', color: '' })

/** 한도 유형 선택 항목 */
const limitTypeItems = [
  { title: '월간', value: 'MONTHLY' },
  { title: '연간', value: 'YEARLY' },
  { title: '커스텀', value: 'CUSTOM' },
]

/** 한도 유형별 칩 색상 */
const limitTypeColor = (typeName) => {
  switch (typeName) {
    case '월간': return 'blue'
    case '연간': return 'green'
    case '커스텀': return 'orange'
    default: return 'grey'
  }
}

/** 금액 포맷 */
const formatAmount = (v) => Number(v).toLocaleString('ko-KR') + '원'

/** 사용률 퍼센트 */
const usagePercent = (limit) => {
  const total = Number(limit.limitAmount)
  if (total <= 0) return 0
  return Math.min(100, (Number(limit.usedAmount) / total) * 100)
}

/** 사용률에 따른 프로그레스 바 색상 */
const usageColor = (limit) => {
  const pct = usagePercent(limit)
  if (pct >= 90) return 'red'
  if (pct >= 70) return 'orange'
  return 'green'
}

/** 카드 확장/축소 토글 */
function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id
}

/** 추가/수정 폼 열기 */
function openForm(item) {
  if (item) {
    editing.value = true
    form.value = {
      creditCardId: item.creditCardId,
      limitType: item.limitType,
      limitAmount: Number(item.limitAmount),
      customStartDate: item.customStartDate || null,
      customEndDate: item.customEndDate || null,
    }
  } else {
    editing.value = false
    form.value = { creditCardId: null, limitType: 'MONTHLY', limitAmount: 0, customStartDate: null, customEndDate: null }
  }
  formDialog.value = true
}

/** 한도 저장 */
async function saveLimit() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  try {
    const payload = {
      creditCardId: form.value.creditCardId,
      limitType: form.value.limitType,
      limitAmount: form.value.limitAmount,
    }
    if (form.value.limitType === 'CUSTOM') {
      payload.customStartDate = form.value.customStartDate
      payload.customEndDate = form.value.customEndDate
    }
    await createOrUpdateLimit(payload)
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
    await deleteLimit(deleteTarget.value.id)
    snackbar.value = { show: true, text: '삭제 완료', color: 'green' }
    deleteDialog.value = false
    load()
  } catch (e) {
    snackbar.value = { show: true, text: '삭제 실패', color: 'red' }
  }
}

/** 한도 목록 로드 */
async function load() {
  loading.value = true
  try {
    const res = await fetchLimits()
    limits.value = res.data
  } catch (e) {
    console.error('한도 목록 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await fetchCreditCards()
    creditCardItems.value = res.data.map(c => ({
      title: `${c.cardCompanyName} ${c.lastFourDigits}`,
      value: c.id,
    }))
  } catch (e) {
    console.error('신용카드 목록 로딩 실패', e)
  }
  load()
})
</script>
