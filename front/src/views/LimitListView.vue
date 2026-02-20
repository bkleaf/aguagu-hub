<template>
  <div>
    <v-row class="mb-4">
      <v-col>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="openForm()">한도 추가</v-btn>
      </v-col>
    </v-row>

    <!-- 한도 목록 -->
    <v-card>
      <v-data-table :headers="headers" :items="limits" :loading="loading">
        <template #item.limitTypeName="{ value }">
          <v-chip size="small" :color="limitTypeColor(value)">{{ value }}</v-chip>
        </template>
        <template #item.limitAmount="{ value }">
          {{ formatAmount(value) }}
        </template>
        <template #item.usedAmount="{ value }">
          {{ formatAmount(value) }}
        </template>
        <template #item.remaining="{ value }">
          {{ formatAmount(value) }}
        </template>
        <template #item.period="{ item }">
          <span v-if="item.limitType === 'CUSTOM' && item.customStartDate">
            {{ item.customStartDate }} ~ {{ item.customEndDate }}
          </span>
          <span v-else>-</span>
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
        <v-card-title>{{ editing ? '한도 수정' : '한도 추가' }}</v-card-title>
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
        <v-card-actions>
          <v-spacer />
          <v-btn @click="formDialog = false">취소</v-btn>
          <v-btn color="primary" @click="saveLimit">저장</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 삭제 확인 다이얼로그 -->
    <v-dialog v-model="deleteDialog" max-width="350">
      <v-card>
        <v-card-title>한도 삭제</v-card-title>
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

const headers = [
  { title: '카드사', key: 'cardCompanyName' },
  { title: '끝4자리', key: 'cardLastFourDigits' },
  { title: '유형', key: 'limitTypeName' },
  { title: '한도', key: 'limitAmount', align: 'end' },
  { title: '사용액', key: 'usedAmount', align: 'end' },
  { title: '잔여', key: 'remaining', align: 'end' },
  { title: '기간', key: 'period', sortable: false },
  { title: '', key: 'actions', sortable: false, width: 100 },
]

const formatAmount = (v) => Number(v).toLocaleString('ko-KR') + '원'

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

function confirmDelete(item) {
  deleteTarget.value = item
  deleteDialog.value = true
}

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
