<template>
  <div>
    <v-row class="mb-4">
      <v-col>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="openForm()">신용카드 등록</v-btn>
      </v-col>
    </v-row>

    <!-- 신용카드 목록 -->
    <v-card>
      <v-data-table :headers="headers" :items="creditCards" :loading="loading">
        <template #item.billingPeriod="{ item }">
          {{ item.billingStartDay }}일 ~ {{ item.billingEndDay }}일
        </template>
        <template #item.paymentDay="{ item }">
          {{ item.paymentDay ? item.paymentDay + '일' : '-' }}
        </template>
        <template #item.active="{ item }">
          <v-icon :color="item.active ? 'green' : 'grey'">
            {{ item.active ? 'mdi-check-circle' : 'mdi-close-circle' }}
          </v-icon>
        </template>
        <template #item.actions="{ item }">
          <v-btn icon="mdi-pencil" size="small" variant="text" @click.stop="openForm(item)" />
          <v-btn icon="mdi-delete" size="small" variant="text" color="red" @click.stop="confirmDelete(item)" />
        </template>
      </v-data-table>
    </v-card>

    <!-- 등록/수정 다이얼로그 -->
    <v-dialog v-model="formDialog" max-width="450">
      <v-card>
        <v-card-title>{{ editing ? '신용카드 수정' : '신용카드 등록' }}</v-card-title>
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
            <v-row>
              <v-col cols="6">
                <v-text-field
                  v-model.number="form.billingStartDay"
                  label="정산 시작일"
                  type="number"
                  min="1"
                  max="31"
                  :rules="[v => v >= 1 && v <= 31 || '1~31']"
                />
              </v-col>
              <v-col cols="6">
                <v-text-field
                  v-model.number="form.billingEndDay"
                  label="정산 종료일"
                  type="number"
                  min="1"
                  max="31"
                  :rules="[v => v >= 1 && v <= 31 || '1~31']"
                />
              </v-col>
            </v-row>
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
            <v-checkbox
              v-model="form.active"
              label="사용"
              color="primary"
              hide-details
            />
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
        <v-card-title>신용카드 삭제</v-card-title>
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

const headers = [
  { title: 'ID', key: 'id' },
  { title: '카드사', key: 'cardCompanyName' },
  { title: '끝4자리', key: 'lastFourDigits' },
  { title: '정산 기간', key: 'billingPeriod', sortable: false },
  { title: '결제일', key: 'paymentDay', sortable: false },
  { title: '사용', key: 'active', sortable: false },
  { title: '', key: 'actions', sortable: false, width: 100 },
]

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

function confirmDelete(item) {
  deleteTarget.value = item
  deleteDialog.value = true
}

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
