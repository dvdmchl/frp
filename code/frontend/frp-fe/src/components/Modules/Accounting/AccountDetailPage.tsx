import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Button,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Spinner,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
  TextInput,
} from 'flowbite-react'
import { AccountingService } from '../../../api/services/AccountingService'
import type { AccAccountDto } from '../../../api/models/AccAccountDto'
import type { AccJournalDto } from '../../../api/models/AccJournalDto'
import type { AccNodeDto } from '../../../api/models/AccNodeDto'
import type { AccTransactionCreateRequestDto } from '../../../api/models/AccTransactionCreateRequestDto'
import type { AccTransactionDto } from '../../../api/models/AccTransactionDto'
import type { ErrorDto } from '../../../api/models/ErrorDto'
import { ApiError } from '../../../api/core/ApiError'
import { ErrorDisplay } from '../../UIComponent/ErrorDisplay'
import { H2Title } from '../../UIComponent/Text'
import { TransactionForm } from './TransactionForm'
import { Paths } from '../../../constants/Paths'

const flattenAccounts = (nodes: AccNodeDto[]): AccAccountDto[] =>
  nodes.flatMap((node) => [node.account, ...flattenAccounts(node.children ?? [])]).filter(Boolean) as AccAccountDto[]

const transactionDate = (transaction: AccTransactionDto) =>
  transaction.journals
    ?.map((journal) => journal.date)
    .filter(Boolean)
    .sort()[0] ?? ''

const formatAmount = (amount: number | undefined, currencyCode: string | undefined) =>
  `${(amount ?? 0).toFixed(2)}${currencyCode ? ` ${currencyCode}` : ''}`

export const AccountDetailPage: React.FC = () => {
  const { t } = useTranslation()
  const { accountId } = useParams()
  const selectedAccountId = Number(accountId)
  const [accounts, setAccounts] = useState<AccAccountDto[]>([])
  const [transactions, setTransactions] = useState<AccTransactionDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ErrorDto | null>(null)
  const [query, setQuery] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [expandedTransactionId, setExpandedTransactionId] = useState<number | null>(null)
  const [transactionModalOpen, setTransactionModalOpen] = useState(false)
  const [editingTransaction, setEditingTransaction] = useState<AccTransactionDto | undefined>()
  const [editingJournal, setEditingJournal] = useState<AccJournalDto | null>(null)
  const [journalDate, setJournalDate] = useState('')
  const [journalDescription, setJournalDescription] = useState('')

  const handleError = useCallback(
    (caughtError: unknown) => {
      if (caughtError instanceof ApiError && caughtError.errorDto) {
        setError(caughtError.errorDto)
      } else {
        setError({ message: t('transaction.error') })
      }
    },
    [t],
  )

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [tree, transactionList] = await Promise.all([
        AccountingService.getTree(),
        AccountingService.getAllTransactions(),
      ])
      setAccounts(flattenAccounts(tree))
      setTransactions(transactionList)
    } catch (caughtError) {
      handleError(caughtError)
    } finally {
      setLoading(false)
    }
  }, [handleError])

  useEffect(() => {
    loadData()
  }, [loadData])

  const account = accounts.find((item) => item.id === selectedAccountId)
  const accountTransactions = useMemo(
    () =>
      transactions.filter((transaction) =>
        transaction.journals?.some((journal) => journal.accountId === selectedAccountId),
      ),
    [selectedAccountId, transactions],
  )
  const filteredTransactions = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase()
    return accountTransactions.filter((transaction) => {
      const date = transactionDate(transaction)
      const matchesQuery =
        !normalizedQuery ||
        transaction.reference?.toLocaleLowerCase().includes(normalizedQuery) ||
        transaction.description?.toLocaleLowerCase().includes(normalizedQuery) ||
        transaction.journals?.some((journal) => journal.description?.toLocaleLowerCase().includes(normalizedQuery))
      return matchesQuery && (!dateFrom || date >= dateFrom) && (!dateTo || date <= dateTo)
    })
  }, [accountTransactions, dateFrom, dateTo, query])

  const selectedAccountTotals = useMemo(
    () =>
      filteredTransactions.reduce(
        (totals, transaction) => {
          transaction.journals
            ?.filter((journal) => journal.accountId === selectedAccountId)
            .forEach((journal) => {
              totals.credit += journal.credit ?? 0
              totals.debit += journal.debit ?? 0
            })
          return totals
        },
        { credit: 0, debit: 0 },
      ),
    [filteredTransactions, selectedAccountId],
  )

  const openCreate = () => {
    setEditingTransaction(undefined)
    setTransactionModalOpen(true)
  }

  const openEdit = (transaction: AccTransactionDto) => {
    setEditingTransaction(transaction)
    setTransactionModalOpen(true)
  }

  const saveTransaction = async (data: AccTransactionCreateRequestDto) => {
    try {
      if (editingTransaction?.id) {
        await AccountingService.updateTransaction(editingTransaction.id, data)
      } else {
        await AccountingService.createTransaction(data)
      }
      setTransactionModalOpen(false)
      await loadData()
    } catch (caughtError) {
      handleError(caughtError)
    }
  }

  const deleteTransaction = async (transaction: AccTransactionDto) => {
    if (!transaction.id || !window.confirm(t('transaction.deleteConfirm'))) return
    try {
      await AccountingService.deleteTransaction(transaction.id)
      await loadData()
    } catch (caughtError) {
      handleError(caughtError)
    }
  }

  const openJournalEdit = (journal: AccJournalDto) => {
    setEditingJournal(journal)
    setJournalDate(journal.date ?? '')
    setJournalDescription(journal.description ?? '')
  }

  const saveJournal = async () => {
    if (!editingJournal?.id || !journalDate) return
    try {
      await AccountingService.updateJournal(editingJournal.id, {
        date: journalDate,
        description: journalDescription || undefined,
      })
      setEditingJournal(null)
      await loadData()
    } catch (caughtError) {
      handleError(caughtError)
    }
  }

  const deleteJournal = async (journal: AccJournalDto) => {
    if (!journal.id || !window.confirm(t('journal.deleteConfirm'))) return
    try {
      await AccountingService.deleteJournal(journal.id)
      await loadData()
    } catch (caughtError) {
      handleError(caughtError)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center p-12">
        <Spinner size="xl" />
      </div>
    )
  }

  if (!Number.isFinite(selectedAccountId) || !account) {
    return (
      <div className="space-y-4 p-4">
        <ErrorDisplay error={error ?? { message: t('account.notFound') }} />
        <Link to={Paths.PARENT} className="text-blue-600 hover:underline">
          {t('account.backToTree')}
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-6 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link to={Paths.PARENT} className="mb-2 inline-block text-sm text-blue-600 hover:underline">
            ← {t('account.backToTree')}
          </Link>
          <H2Title>{account.name}</H2Title>
          {account.description && <p className="text-gray-600">{account.description}</p>}
        </div>
        <Button onClick={openCreate}>{t('transaction.createTitle')}</Button>
      </div>

      {error && <ErrorDisplay error={error} />}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard label={t('account.balance')} value={formatAmount(account.balance, account.currencyCode)} />
        <SummaryCard label={t('transaction.count')} value={filteredTransactions.length.toString()} />
        <SummaryCard
          label={t('transaction.totalDebit')}
          value={formatAmount(selectedAccountTotals.debit, account.currencyCode)}
        />
        <SummaryCard
          label={t('transaction.totalCredit')}
          value={formatAmount(selectedAccountTotals.credit, account.currencyCode)}
        />
      </div>

      <div className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4 md:grid-cols-3">
        <div>
          <Label htmlFor="transaction-search">{t('transaction.search')}</Label>
          <TextInput
            id="transaction-search"
            value={query}
            placeholder={t('transaction.searchPlaceholder')}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="transaction-date-from">{t('transaction.dateFrom')}</Label>
          <TextInput
            id="transaction-date-from"
            type="date"
            value={dateFrom}
            onChange={(event) => setDateFrom(event.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="transaction-date-to">{t('transaction.dateTo')}</Label>
          <TextInput
            id="transaction-date-to"
            type="date"
            value={dateTo}
            onChange={(event) => setDateTo(event.target.value)}
          />
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell>{t('transaction.date')}</TableHeadCell>
              <TableHeadCell>{t('transaction.reference')}</TableHeadCell>
              <TableHeadCell>{t('transaction.description')}</TableHeadCell>
              <TableHeadCell>{t('transaction.amount')}</TableHeadCell>
              <TableHeadCell>{t('common.actions')}</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {filteredTransactions.map((transaction) => (
              <React.Fragment key={transaction.id}>
                <TableRow className="cursor-pointer bg-white hover:bg-gray-50">
                  <TableCell
                    onClick={() =>
                      setExpandedTransactionId(
                        expandedTransactionId === transaction.id ? null : (transaction.id ?? null),
                      )
                    }
                  >
                    {transactionDate(transaction)}
                  </TableCell>
                  <TableCell
                    onClick={() =>
                      setExpandedTransactionId(
                        expandedTransactionId === transaction.id ? null : (transaction.id ?? null),
                      )
                    }
                  >
                    {transaction.reference || '—'}
                  </TableCell>
                  <TableCell
                    onClick={() =>
                      setExpandedTransactionId(
                        expandedTransactionId === transaction.id ? null : (transaction.id ?? null),
                      )
                    }
                  >
                    {transaction.description || '—'}
                  </TableCell>
                  <TableCell>{formatAmount(transaction.totalAmount, account.currencyCode)}</TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-2">
                      <Button size="xs" color="light" onClick={() => openEdit(transaction)}>
                        {t('common.edit')}
                      </Button>
                      <Button size="xs" color="failure" onClick={() => deleteTransaction(transaction)}>
                        {t('common.delete')}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
                {expandedTransactionId === transaction.id && (
                  <TableRow>
                    <TableCell colSpan={5} className="bg-gray-50 p-3">
                      <JournalTable
                        journals={transaction.journals ?? []}
                        accounts={accounts}
                        onEdit={openJournalEdit}
                        onDelete={deleteJournal}
                      />
                    </TableCell>
                  </TableRow>
                )}
              </React.Fragment>
            ))}
            {filteredTransactions.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="py-8 text-center text-gray-500">
                  {t('transaction.empty')}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <Modal show={transactionModalOpen} size="7xl" onClose={() => setTransactionModalOpen(false)}>
        <ModalBody>
          <TransactionForm
            key={editingTransaction?.id ?? 'create'}
            accounts={accounts}
            initialData={editingTransaction}
            selectedAccountId={selectedAccountId}
            onSubmit={saveTransaction}
            onCancel={() => setTransactionModalOpen(false)}
          />
        </ModalBody>
      </Modal>

      <Modal show={Boolean(editingJournal)} onClose={() => setEditingJournal(null)}>
        <ModalHeader>{t('journal.editTitle')}</ModalHeader>
        <ModalBody>
          <div className="space-y-4">
            <div>
              <Label htmlFor="journal-edit-date">{t('journal.date')}</Label>
              <TextInput
                id="journal-edit-date"
                type="date"
                value={journalDate}
                onChange={(event) => setJournalDate(event.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="journal-edit-description">{t('journal.description')}</Label>
              <TextInput
                id="journal-edit-description"
                value={journalDescription}
                onChange={(event) => setJournalDescription(event.target.value)}
              />
            </div>
          </div>
        </ModalBody>
        <ModalFooter>
          <Button onClick={saveJournal} disabled={!journalDate}>
            {t('journal.update')}
          </Button>
          <Button color="gray" onClick={() => setEditingJournal(null)}>
            {t('common.close')}
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  )
}

const SummaryCard: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
    <div className="text-sm font-medium text-gray-500">{label}</div>
    <div className="mt-1 text-2xl font-bold text-gray-900">{value}</div>
  </div>
)

interface JournalTableProps {
  journals: AccJournalDto[]
  accounts: AccAccountDto[]
  onEdit: (journal: AccJournalDto) => void
  onDelete: (journal: AccJournalDto) => void
}

const JournalTable: React.FC<JournalTableProps> = ({ journals, accounts, onEdit, onDelete }) => {
  const { t } = useTranslation()
  const accountNames = new Map(accounts.map((account) => [account.id, account.name]))

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHead>
          <TableRow>
            <TableHeadCell>{t('journal.date')}</TableHeadCell>
            <TableHeadCell>{t('journal.account')}</TableHeadCell>
            <TableHeadCell>{t('journal.description')}</TableHeadCell>
            <TableHeadCell>{t('journal.debit')}</TableHeadCell>
            <TableHeadCell>{t('journal.credit')}</TableHeadCell>
            <TableHeadCell>{t('common.actions')}</TableHeadCell>
          </TableRow>
        </TableHead>
        <TableBody className="divide-y">
          {journals.map((journal) => (
            <TableRow key={journal.id} className="bg-white">
              <TableCell>{journal.date}</TableCell>
              <TableCell>{accountNames.get(journal.accountId) ?? journal.accountId}</TableCell>
              <TableCell>{journal.description || '—'}</TableCell>
              <TableCell>{(journal.debit ?? 0).toFixed(2)}</TableCell>
              <TableCell>{(journal.credit ?? 0).toFixed(2)}</TableCell>
              <TableCell>
                <div className="flex flex-wrap gap-2">
                  <Button size="xs" color="light" onClick={() => onEdit(journal)}>
                    {t('common.edit')}
                  </Button>
                  <Button size="xs" color="failure" onClick={() => onDelete(journal)}>
                    {t('common.delete')}
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
