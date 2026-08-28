import React, { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Label, Select, Textarea, TextInput } from 'flowbite-react'
import type { AccAccountDto } from '../../../api/models/AccAccountDto'
import type { AccTransactionCreateRequestDto } from '../../../api/models/AccTransactionCreateRequestDto'
import type { AccTransactionDto } from '../../../api/models/AccTransactionDto'
import { H3Title } from '../../UIComponent/Text'

interface TransactionFormProps {
  accounts: AccAccountDto[]
  initialData?: AccTransactionDto
  selectedAccountId: number
  onSubmit: (data: AccTransactionCreateRequestDto) => Promise<void>
  onCancel: () => void
}

interface JournalDraft {
  date: string
  description: string
  accountId: string
  credit: string
  debit: string
}

const emptyJournal = (accountId?: number): JournalDraft => ({
  date: new Date().toISOString().slice(0, 10),
  description: '',
  accountId: accountId?.toString() ?? '',
  credit: '',
  debit: '',
})

export const TransactionForm: React.FC<TransactionFormProps> = ({
  accounts,
  initialData,
  selectedAccountId,
  onSubmit,
  onCancel,
}) => {
  const { t } = useTranslation()
  const [reference, setReference] = useState(initialData?.reference ?? '')
  const [description, setDescription] = useState(initialData?.description ?? '')
  const [fxRate, setFxRate] = useState(initialData?.fxRate?.toString() ?? '1')
  const [submitting, setSubmitting] = useState(false)
  const [journals, setJournals] = useState<JournalDraft[]>(
    initialData?.journals?.length
      ? initialData.journals.map((journal) => ({
          date: journal.date ?? new Date().toISOString().slice(0, 10),
          description: journal.description ?? '',
          accountId: journal.accountId?.toString() ?? '',
          credit: journal.credit ? journal.credit.toString() : '',
          debit: journal.debit ? journal.debit.toString() : '',
        }))
      : [emptyJournal(selectedAccountId), emptyJournal()],
  )

  const totals = useMemo(
    () =>
      journals.reduce(
        (sum, journal) => ({
          credit: sum.credit + (Number(journal.credit) || 0),
          debit: sum.debit + (Number(journal.debit) || 0),
        }),
        { credit: 0, debit: 0 },
      ),
    [journals],
  )

  const entriesValid = journals.every((journal) => {
    const credit = Number(journal.credit) || 0
    const debit = Number(journal.debit) || 0
    return Boolean(journal.date && journal.accountId) && ((credit > 0 && debit === 0) || (debit > 0 && credit === 0))
  })
  const isBalanced = totals.credit > 0 && Math.abs(totals.credit - totals.debit) < 0.000001
  const canSubmit = journals.length >= 2 && entriesValid && isBalanced && Number(fxRate) > 0

  const updateJournal = (index: number, field: keyof JournalDraft, value: string) => {
    setJournals((current) =>
      current.map((journal, journalIndex) => (journalIndex === index ? { ...journal, [field]: value } : journal)),
    )
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!canSubmit) return

    setSubmitting(true)
    try {
      await onSubmit({
        reference: reference || undefined,
        description: description || undefined,
        fxRate: Number(fxRate),
        journals: journals.map((journal) => ({
          date: journal.date,
          description: journal.description || undefined,
          accountId: Number(journal.accountId),
          credit: Number(journal.credit) || 0,
          debit: Number(journal.debit) || 0,
        })),
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <H3Title>{initialData ? t('transaction.editTitle') : t('transaction.createTitle')}</H3Title>

      <div className="grid gap-4 md:grid-cols-3">
        <div>
          <Label htmlFor="transaction-reference">{t('transaction.reference')}</Label>
          <TextInput
            id="transaction-reference"
            value={reference}
            onChange={(event) => setReference(event.target.value)}
          />
        </div>
        <div className="md:col-span-2">
          <Label htmlFor="transaction-description">{t('transaction.description')}</Label>
          <Textarea
            id="transaction-description"
            rows={1}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="transaction-fx-rate">{t('transaction.fxRate')}</Label>
          <TextInput
            id="transaction-fx-rate"
            type="number"
            min="0.000001"
            step="any"
            value={fxRate}
            onChange={(event) => setFxRate(event.target.value)}
          />
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h4 className="text-lg font-semibold text-gray-900">{t('journal.title')}</h4>
          <Button size="xs" color="light" type="button" onClick={() => setJournals([...journals, emptyJournal()])}>
            {t('journal.add')}
          </Button>
        </div>

        {journals.map((journal, index) => (
          <div key={index} className="grid gap-3 rounded-lg border border-gray-200 p-3 md:grid-cols-12">
            <div className="md:col-span-2">
              <Label htmlFor={`journal-date-${index}`}>{t('journal.date')}</Label>
              <TextInput
                id={`journal-date-${index}`}
                type="date"
                value={journal.date}
                onChange={(event) => updateJournal(index, 'date', event.target.value)}
              />
            </div>
            <div className="md:col-span-3">
              <Label htmlFor={`journal-account-${index}`}>{t('journal.account')}</Label>
              <Select
                id={`journal-account-${index}`}
                value={journal.accountId}
                onChange={(event) => updateJournal(index, 'accountId', event.target.value)}
              >
                <option value="">{t('common.none')}</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name} ({account.currencyCode})
                  </option>
                ))}
              </Select>
            </div>
            <div className="md:col-span-2">
              <Label htmlFor={`journal-credit-${index}`}>{t('journal.credit')}</Label>
              <TextInput
                id={`journal-credit-${index}`}
                type="number"
                min="0"
                step="any"
                value={journal.credit}
                onChange={(event) => updateJournal(index, 'credit', event.target.value)}
              />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor={`journal-debit-${index}`}>{t('journal.debit')}</Label>
              <TextInput
                id={`journal-debit-${index}`}
                type="number"
                min="0"
                step="any"
                value={journal.debit}
                onChange={(event) => updateJournal(index, 'debit', event.target.value)}
              />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor={`journal-description-${index}`}>{t('journal.description')}</Label>
              <TextInput
                id={`journal-description-${index}`}
                value={journal.description}
                onChange={(event) => updateJournal(index, 'description', event.target.value)}
              />
            </div>
            <div className="flex items-end md:col-span-1">
              <Button
                size="xs"
                color="failure"
                type="button"
                disabled={journals.length <= 2}
                onClick={() => setJournals(journals.filter((_, journalIndex) => journalIndex !== index))}
              >
                {t('common.delete')}
              </Button>
            </div>
          </div>
        ))}
      </div>

      <div
        className={`rounded-lg p-3 text-sm ${isBalanced ? 'bg-green-50 text-green-700' : 'bg-amber-50 text-amber-800'}`}
      >
        {t('transaction.balanceSummary', { credit: totals.credit.toFixed(2), debit: totals.debit.toFixed(2) })}
      </div>

      <div className="flex justify-end gap-2">
        <Button color="gray" type="button" onClick={onCancel}>
          {t('common.close')}
        </Button>
        <Button type="submit" disabled={!canSubmit || submitting}>
          {submitting ? '...' : t(initialData ? 'transaction.update' : 'transaction.create')}
        </Button>
      </div>
    </form>
  )
}
