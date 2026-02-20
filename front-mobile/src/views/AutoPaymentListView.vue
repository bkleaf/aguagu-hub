<template>
  <div>
    <!-- 추가 버튼 + 합계 -->
    <div class="d-flex justify-space-between align-center mb-3">
      <v-btn color="primary" prepend-icon="mdi-plus" size="small" @click="openForm()">추가</v-btn>
      <v-chip color="blue" variant="tonal" size="small">합계: {{ formatAmount(totalAmount) }}</v-chip>
    </div>

    <!-- 카드 필터 -->
    <v-select
      v-model="filterCardId"
      :items="creditCardFilterItems"
      label="카드 필터"
      density="compact"
      clearable
      hide-details
      class="mb-3"
      @update:model-value="load"
    />

    <!-- 로딩 -->
    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-2" />

    <!-- 자동결제 카드 리스트 -->
    <v-card
      v-for="ap in autoPayments"
      :key="ap.id"
      class="mb-2"
      variant="outlined"
    >
      <v-card-text class="py-2">
        <div class="d-flex justify-space-between align-center">
          <div style="min-width: 0; flex: 1">
            <div class="text-body-2 font-weight-medium text-truncate">{{ ap.description }}</div>
            <div class="text-caption text-grey">
              {{ ap.cardCompanyName }} ({{ ap.cardLastFourDigits }}) &middot; 매월 {{ ap.paymentDay }}일
            </div>
          </div>
          <div class="d-flex align-center ml-2">
            <v-chip size="x-small" :color="ap.active ? 'green' : 'grey'" class="mr-1">
              {{ ap.active ? '활성' : '비활성' }}
            </v-chip>
            <span class="text-body-2 font-weight-bold text-no-wrap">{{ formatAmount(ap.amount) }}</span>
          </div>
        </div>
      </v-card-text>
      <v-card-actions class="pt-0">
        <v-spacer />
        <v-btn size="small" variant="text" icon="mdi-pencil" @click="openForm(ap)" />
        <v-btn size="small" variant="text" icon="mdi-delete" color="red" @click="confirmDelete(ap)" />
      </v-card-actions>
    </v-card>

    <v-card v-if="!loading && autoPayments.length === 0" variant="outlined">
      <v-card-text class="text-center text-grey py-6">등록된 자동결제가 없습니다</v-card-text>
    </v-card>

    <!-- 추가/수정 다이얼로그 (풀스크린) -->
    <v-dialog v-model="formDialog" fullscreen transition="dialog-bottom-transition">
      <v-card>
        <v-toolbar color="primary" density="compact">
          <v-btn icon="mdi-close" @click="formDialog = false" />
          <v-toolbar-title class="text-body-1">{{ editing ? '자동결제 수정' : '자동결제 추가' }}</v-toolbar-title>
          <v-spacer />
          <v-btn variant="text" @click="save">저장</v-btn>
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
            <v-text-field
              v-model="form.description"
              label="설명 (예: 넷플릭스, SKT)"
              :rules="[v => !!v || '필수', v => (v && v.length <= 100) || '100자 이내']"
            />
            <v-text-field
              v-model.number="form.amount"
              label="결제 금액 (원)"
              type="number"
              :rules="[v => v > 0 || '양수여야 합니다']"
            />
            <v-text-field
              v-model.number="form.paymentDay"
              label="매월 결제일 (1~31)"
              type="number"
              :rules="[v => v >= 1 && v <= 31 || '1~31 사이여야 합니다']"
            />
            <v-switch
              v-model="form.active"
              label="활성화"
              color="primary"
            />
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- 삭제 확인 다이얼로그 -->
    <v-dialog v-model="deleteDialog" max-width="320">
      <v-card>
        <v-card-title class="text-body-1">자동결제 삭제</v-card-title>
        <v-card-text>
          "{{ deleteTarget?.description }}" ({{ deleteTarget?.cardCompanyName }} {{ deleteTarget?.cardLastFourDigits }})
          자동결제를 삭제하시겠습니까?
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
import { ref, computed, onMounted } from 'vue'
import {
  fetchAutoPayments,
  fetchAutoPaymentsByCard,
  createAutoPayment,
  updateAutoPayment,
  deleteAutoPayment,
  fetchCreditCards
} from '../api'

const loading = ref(false)
const autoPayments = ref([])
const creditCardItems = ref([])
const creditCardFilterItems = ref([])
const filterCardId = ref(null)
const formDialog = ref(false)
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = ref({ creditCardId: null, description: '', amount: 0, paymentDay: 1, active: true })
const snackbar = ref({ show: false, text: '', color: '' })

/** 금액 포맷 */
const formatAmount = (v) => Number(v).toLocaleString('ko-KR') + '원'

/** 활성 자동결제 금액 합계 */
const totalAmount = computed(() => {
  return autoPayments.value
    .filter(a => a.active)
    .reduce((sum, a) => sum + Number(a.amount), 0)
})

/** 추가/수정 폼 열기 */
function openForm(item) {
  if (item) {
    editing.value = true
    editingId.value = item.id
    form.value = {
      creditCardId: item.creditCardId,
      description: item.description,
      amount: Number(item.amount),
      paymentDay: item.paymentDay,
      active: item.active,
    }
  } else {
    editing.value = false
    editingId.value = null
    form.value = { creditCardId: null, description: '', amount: 0, paymentDay: 1, active: true }
  }
  formDialog.value = true
}

/** 저장 */
async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  try {
    const payload = {
      creditCardId: form.value.creditCardId,
      description: form.value.description,
      amount: form.value.amount,
      paymentDay: form.value.paymentDay,
      active: form.value.active,
    }
    if (editing.value) {
      await updateAutoPayment(editingId.value, payload)
    } else {
      await createAutoPayment(payload)
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
    await deleteAutoPayment(deleteTarget.value.id)
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
    const res = filterCardId.value
      ? await fetchAutoPaymentsByCard(filterCardId.value)
      : await fetchAutoPayments()
    autoPayments.value = res.data
  } catch (e) {
    console.error('자동결제 목록 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await fetchCreditCards()
    const cards = res.data
    creditCardItems.value = cards.map(c => ({
      title: `${c.cardCompanyName} ${c.lastFourDigits}`,
      value: c.id,
    }))
    creditCardFilterItems.value = cards.map(c => ({
      title: `${c.cardCompanyName} ${c.lastFourDigits}`,
      value: c.id,
    }))
  } catch (e) {
    console.error('신용카드 목록 로딩 실패', e)
  }
  load()
})
</script>
