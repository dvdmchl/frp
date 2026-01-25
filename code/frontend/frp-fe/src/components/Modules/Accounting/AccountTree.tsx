import React, { useEffect, useState, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Modal, ModalBody, Spinner } from 'flowbite-react'
import { AccountingService } from '../../../api/services/AccountingService'
import type { AccNodeDto } from '../../../api/models/AccNodeDto'
import type { AccAccountCreateRequestDto } from '../../../api/models/AccAccountCreateRequestDto'
import type { AccCurrencyDto } from '../../../api/models/AccCurrencyDto'
import { AccountForm } from './AccountForm'
import { ErrorDisplay } from '../../UIComponent/ErrorDisplay'
import { ErrorBoundary } from '../../UIComponent/ErrorBoundary'
import { ApiError } from '../../../api/core/ApiError'
import { H2Title } from '../../UIComponent/Text'

interface TreeNodeProps {
  node: AccNodeDto
  onEdit: (node: AccNodeDto) => void
  onDelete: (node: AccNodeDto) => void
  onAddChild: (parentId: number) => void
  onMove: (node: AccNodeDto, direction: 'up' | 'down') => void
  isFirst: boolean
  isLast: boolean
}

const TreeNode: React.FC<TreeNodeProps> = ({ node, onEdit, onDelete, onAddChild, onMove, isFirst, isLast }) => {
  const [expanded, setExpanded] = useState(true)
  const { t } = useTranslation()
  const hasChildren = node.children && node.children.length > 0

  return (
    <div className="border-l border-gray-100 ml-2">
      <div className="flex items-center p-2 hover:bg-gray-100 border-b border-gray-100 pl-2">
        <div className="mr-2 w-4">
          {hasChildren && (
            <button onClick={() => setExpanded(!expanded)} className="focus:outline-none font-bold text-gray-500">
              {expanded ? '▼' : '▶'}
            </button>
          )}
        </div>

        <div className="flex-grow flex items-center gap-2">
          <span className={`font-medium ${node.isPlaceholder ? 'text-gray-500 italic' : 'text-gray-900'}`}>
            {node.account?.name || 'Unnamed'}
          </span>
          {!node.isPlaceholder && (
            <span className="text-sm text-gray-500 bg-gray-100 px-2 py-0.5 rounded">{node.account?.currencyCode}</span>
          )}
          <span className="text-xs text-gray-400">({t(`account.types.${node.account?.accountType}`)})</span>
        </div>

        {!node.isPlaceholder && <div className="mr-4 font-mono font-semibold">{node.account?.balance?.toFixed(2)}</div>}

        <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          {/* Add Child */}
          <Button size="xs" color="gray" onClick={() => node.id && onAddChild(node.id)}>
            +
          </Button>

          {/* Move Up/Down */}
          <div className="flex flex-col">
            <button
              disabled={isFirst}
              onClick={() => onMove(node, 'up')}
              className={`text-xs px-1 ${isFirst ? 'text-gray-300' : 'text-gray-600 hover:text-black'}`}
            >
              ▲
            </button>
            <button
              disabled={isLast}
              onClick={() => onMove(node, 'down')}
              className={`text-xs px-1 ${isLast ? 'text-gray-300' : 'text-gray-600 hover:text-black'}`}
            >
              ▼
            </button>
          </div>

          {/* Edit */}
          <Button size="xs" color="light" onClick={() => onEdit(node)}>
            {t('common.edit')}
          </Button>
          {/* Delete */}
          <Button size="xs" color="failure" onClick={() => onDelete(node)}>
            X
          </Button>
        </div>
      </div>

      {expanded && hasChildren && (
        <div className="pl-4">
          {node.children?.map((child: AccNodeDto, index: number) => (
            <TreeNode
              key={child.id}
              node={child}
              onEdit={onEdit}
              onDelete={onDelete}
              onAddChild={onAddChild}
              onMove={onMove}
              isFirst={index === 0}
              isLast={index === (node.children?.length || 0) - 1}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export const AccountTree: React.FC = () => {
  const { t } = useTranslation()
  const [tree, setTree] = useState<AccNodeDto[]>([])
  const [currencies, setCurrencies] = useState<AccCurrencyDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ApiError | null>(null)

  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editNode, setEditNode] = useState<AccNodeDto | null>(null) // If null, we are creating
  const [parentForNew, setParentForNew] = useState<number | undefined>(undefined)

  const fetchData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [treeData, currencyData] = await Promise.all([
        AccountingService.getTree(),
        AccountingService.getAllCurrencies(),
      ])
      setTree(treeData)
      setCurrencies(currencyData)
    } catch (err) {
      setError(err as ApiError)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleCreate = (parentId?: number) => {
    setEditNode(null)
    setParentForNew(parentId)
    setIsModalOpen(true)
  }

  const handleEdit = (node: AccNodeDto) => {
    setEditNode(node)
    setParentForNew(undefined)
    setIsModalOpen(true)
  }

  const handleDelete = async (node: AccNodeDto) => {
    if (!node.id) return
    if (!window.confirm(t('account.deleteConfirm'))) return

    try {
      await AccountingService.deleteAccount(node.id)
      fetchData()
    } catch {
      alert(t('account.error'))
    }
  }

  const handleMove = async (node: AccNodeDto, direction: 'up' | 'down') => {
    if (!node.id || node.orderIndex === undefined) return

    const parent = findParent(tree, node.id)
    const siblings = parent ? parent.children : tree
    if (!siblings) return

    const currentIndex = siblings.findIndex((s: AccNodeDto) => s.id === node.id)
    if (currentIndex === -1) return

    const targetIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1
    if (targetIndex < 0 || targetIndex >= siblings.length) return

    await AccountingService.moveAccount(node.id, {
      newParentId: parent?.id,
      newOrderIndex: targetIndex,
    })
    fetchData()
  }

  const findParent = (nodes: AccNodeDto[], childId: number): AccNodeDto | null => {
    for (const node of nodes) {
      if (node.children?.some((c: AccNodeDto) => c.id === childId)) {
        return node
      }
      if (node.children) {
        const found = findParent(node.children, childId)
        if (found) return found
      }
    }
    return null
  }

  const handleSubmit = async (data: AccAccountCreateRequestDto) => {
    try {
      if (editNode?.id) {
        await AccountingService.updateAccount(editNode.id, data)
      } else {
        await AccountingService.createAccount(data)
      }
      setIsModalOpen(false)
      fetchData()
    } catch (err) {
      setError(err as ApiError)
    }
  }

  const getFlattenedParents = (nodes: AccNodeDto[]): { id: number; name: string }[] => {
    let result: { id: number; name: string }[] = []
    for (const node of nodes) {
      if (node.id && node.account?.name) {
        result.push({ id: node.id, name: node.account.name })
      }
      if (node.children) {
        result = [...result, ...getFlattenedParents(node.children)]
      }
    }
    return result
  }

  const possibleParents = getFlattenedParents(tree)

  const initialFormData = React.useMemo(() => {
    return editNode
      ? { ...editNode.account, parentId: editNode.parentId, isPlaceholder: editNode.isPlaceholder }
      : { parentId: parentForNew }
  }, [editNode, parentForNew])

  if (loading) return <Spinner />
  if (error) {
    const errorDto = (error as any).errorDto || { message: error.message, stackTrace: error.stack }
    return <ErrorDisplay error={errorDto} />
  }

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-4">
        <H2Title>{t('account.title')}</H2Title>
        <Button onClick={() => handleCreate(undefined)}>{t('account.create')}</Button>
      </div>

      <div className="bg-white rounded shadow overflow-hidden group">
        {tree.map((node, index) => (
          <TreeNode
            key={node.id}
            node={node}
            onEdit={handleEdit}
            onDelete={handleDelete}
            onAddChild={handleCreate}
            onMove={handleMove}
            isFirst={index === 0}
            isLast={index === tree.length - 1}
          />
        ))}
        {tree.length === 0 && <div className="p-4 text-center text-gray-500">No accounts found.</div>}
      </div>

      <Modal show={isModalOpen} onClose={() => setIsModalOpen(false)}>
        <ModalBody>
          <ErrorBoundary>
            <AccountForm
              initialData={initialFormData}
              currencies={currencies}
              possibleParents={possibleParents}
              onSubmit={handleSubmit}
              onCancel={() => setIsModalOpen(false)}
            />
          </ErrorBoundary>
        </ModalBody>
      </Modal>
    </div>
  )
}