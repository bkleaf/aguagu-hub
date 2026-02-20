<template>
  <div>
    <!-- 카드 등록 버튼 -->
    <v-btn color="primary" prepend-icon="mdi-plus" block class="mb-3" @click="openForm()">신용카드 등록</v-btn>

    <!-- 로딩 -->
    <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-2" />

    <!-- 신용카드 카드형 리스트 -->
    <v-card
      v-for="card in creditCards"
      :key="card.id"
      class="mb-3"
      variant="outlined"
    >
      <v-card-text>
        <div class="d-flex justify-space-between align-center mb-2">
          <div>
            <div class="text-body-1 font-weight-bold">{{ card.cardCompanyName }}</div>
            <div class="text-h6 font-weight-medium">**** {{ card.lastFourDigits }}</div>
          </div>
          <v-icon :color="card.active ? 'green' : 'grey'" size="28">
            {{ card.active ? 'mdi-check-circle' : 'mdi-close-circle' }}
          </v-icon>
        </div>
        <v-divider class="mb-2" />
        <div class="d-flex justify-space-between text-caption text-grey">
          <span>정산: {{ card.billingStartDay }}일 ~ {{ card.billingEndDay }}일</span>
          <span>결제일: {{ card.paymentDay ? card.paymentDay + '일' : '-' }}</span>
        </div>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn size="small" variant="text" icon="mdi-pencil" @click="openForm(card)" />
        <v-btn size="small" variant="text" icon="mdi-delete" color="red" @click="confirmDelete(card)" />
      </v-card-actions>
    </v-card>

    <v-card v-if="!loading && creditCards.length === 0" variant="outlined">
      <v-card-text class="text-center text-grey py-6">등록된 신용카드가 없습니다</v-card-text>
    </v-card>

    <!-- 등록/수정 다이얼로그 (풀스크린) -->
    <v-dialog v-model="formDialog" fullscreen transition="dialog-bottom-transition">
      <v-card>
        <v-toolbar color="primary" density="compact">
          <v-btn icon="mdi-close" @click="formDialog = false" />
          <v-toolbar-title class="text-body-1">{{ editing ? '신용카드 수정' : '신용카드 등록' }}</v-toolbar-title>
          <v-spacer />
          <v-btn variant="text" @click="save">저장</v-btn>
        </v-toolbar>
        <v-card-text>
          <v-form ref="formRef">
            <v-select
              v-model="form.cardCompany"
              :items="companyItems"
              label="카드사"
              :rules="[v => !!v || '필수']"
              :disabled="editing"
            />
            <v-text-field
              v-model="form.lastFourDigits"
              label="카드 끝4자리"
              maxlength="4"
              :rules="[v => !!v || '필수', v => /^\d{4}$/.test(v) || '숫자 4자리']"
              :disabled="editing"
            />
            <v-text-field
              v-model.number="form.billingStartDay"
              label="정산 시작일"
              type="number"
              min="1"
              max="31"
              :rules="[v => v >= 1 && v <= 31 || '1~31']"
            />
            <v-text-field
              v-model.number="form.billingEndDay"
              label="정산 종료일"
              type="number"
              min="1"
              max="31"
              :rules="[v => v >= 1 && v <= 31 || '1~31']"
            />
            <v-text-field
              v-model.number="form.paymentDay"
              label="결제일 (매월 N일)"
              type="number"
              min="1"
              max="31"
              :rules="[v => !v || (v >= 1 && v <= 31) || '1~31']"
              clearable
              hint="선택사항"
              persistent-hint
            />
            <v-switch
              v-model="form.active"
              label="사용"
              color="primary"
            />
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- 삭제 확인 다이얼로그 -->
    <v-dialog v-model="deleteDialog" max-width="320">
      <v-card>
        <v-card-title class="text-body-1">신용카드 삭제</v-card-title>
        <v-card-text>{{ deleteTarget?.cardCompanyName }} ({{ deleteTarget?.lastFourDigits }})를 삭제하시겠습니까?</v-card-text>
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
import { fetchCreditCards, registerCreditCard, deleteCreditCard, fetchSupportedCompanies } from '../api'

const loading = ref(false)
const creditCards = ref([])
const formDialog = ref(false)
const deleteDialog = ref(false)
const deleteTarget = ref(null)
const editing = ref(false)
const formRef = ref(null)
const form = ref({ cardCompany: null, lastFourDigits: '', billingStartDay: 1, billingEndDay: 31, paymentDay: null, active: true })
const snackbar = ref({ show: false, text: '', color: '' })
const companyItems = ref([])

/** 등록/수정 폼 열기 */
function openForm(item) {
  if (item) {
    editing.value = true
    form.value = {
      cardCompany: item.cardCompany,
      lastFourDigits: item.lastFourDigits,
      billingStartDay: item.billingStartDay,
      billingEndDay: item.billingEndDay,
      paymentDay: item.paymentDay,
      active: item.active,
    }
  } else {
    editing.value = false
    form.value = { cardCompany: null, lastFourDigits: '', billingStartDay: 1, billingEndDay: 31, paymentDay: null, active: true }
  }
  formDialog.value = true
}

/** 저장 */
async function save() {
  const { valid } = await formRef.value.validate()
  if (!valid) return
  try {
    await registerCreditCard(form.value)
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
    await deleteCreditCard(deleteTarget.value.id)
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
    const res = await fetchCreditCards()
    creditCards.value = res.data
  } catch (e) {
    console.error('신용카드 목록 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await fetchSupportedCompanies()
  companyItems.value = res.data.map(c => ({ title: c.name, value: c.code }))
  load()
})
</script>
