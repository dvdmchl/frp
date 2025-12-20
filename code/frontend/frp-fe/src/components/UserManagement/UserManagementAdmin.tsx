import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
  Button,
  TextInput,
  Badge,
  Spinner,
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  ToggleSwitch,
  Label,
} from 'flowbite-react'
import { UserManagementAdminService, ApiError, type UserDto } from '../../api'
import { ErrorDisplay } from '../UIComponent/ErrorDisplay'
import { H1Title } from '../UIComponent/Text'

export function UserManagementAdmin() {
  const { t } = useTranslation()
  const [users, setUsers] = useState<UserDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ApiError | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedUser, setSelectedUser] = useState<UserDto | null>(null)
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false)

  const fetchUsers = useCallback(async (query?: string) => {
    setLoading(true)
    setError(null)
    try {
      const data = await UserManagementAdminService.listUsers(query)
      setUsers(data)
    } catch (err) {
      setError(err as ApiError)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchUsers()
  }, [fetchUsers])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    fetchUsers(searchQuery)
  }

  const handleToggleActive = async (user: UserDto) => {
    try {
      const updatedUser = await UserManagementAdminService.updateActiveStatus(user.id!, !user.active)
      setUsers((prev) => prev.map((u) => (u.id === updatedUser.id ? updatedUser : u)))
      if (selectedUser?.id === updatedUser.id) {
        setSelectedUser(updatedUser)
      }
    } catch (err) {
      setError(err as ApiError)
    }
  }

  const handleToggleAdmin = async (user: UserDto) => {
    try {
      const updatedUser = await UserManagementAdminService.updateAdminStatus(user.id!, !user.admin)
      setUsers((prev) => prev.map((u) => (u.id === updatedUser.id ? updatedUser : u)))
      if (selectedUser?.id === updatedUser.id) {
        setSelectedUser(updatedUser)
      }
    } catch (err) {
      setError(err as ApiError)
    }
  }

  const openDetails = (user: UserDto) => {
    setSelectedUser(user)
    setIsDetailsModalOpen(true)
  }

  return (
    <div className="p-4 space-y-6">
      <div className="flex justify-between items-center">
        <H1Title>{t('admin.userManagement.title')}</H1Title>
      </div>

      <ErrorDisplay error={error} />

      <form onSubmit={handleSearch} className="flex gap-2">
        <TextInput
          id="search"
          type="text"
          placeholder={t('admin.userManagement.searchPlaceholder')}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="flex-grow"
        />
        <Button type="submit" disabled={loading}>
          {t('common.search')}
        </Button>
        <Button
          color="gray"
          onClick={() => {
            setSearchQuery('')
            fetchUsers()
          }}
          disabled={loading}
        >
          {t('common.reset')}
        </Button>
      </form>

      {loading ? (
        <div className="flex justify-center p-10">
          <Spinner size="xl" />
        </div>
      ) : (
        <div className="overflow-x-auto">
          <Table hoverable>
            <TableHead>
              <TableRow>
                <TableHeadCell>{t('user.fullName')}</TableHeadCell>
                <TableHeadCell>{t('user.email')}</TableHeadCell>
                <TableHeadCell>{t('admin.userManagement.status')}</TableHeadCell>
                <TableHeadCell>{t('admin.userManagement.role')}</TableHeadCell>
                <TableHeadCell>
                  <span className="sr-only">{t('common.actions')}</span>
                </TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody className="divide-y">
              {users.map((user) => (
                <TableRow key={user.id} className="bg-white dark:border-gray-700 dark:bg-gray-800">
                  <TableCell className="whitespace-nowrap font-medium text-gray-900 dark:text-white">
                    {user.fullName}
                  </TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>
                    {user.active ? (
                      <Badge color="success">{t('admin.userManagement.active')}</Badge>
                    ) : (
                      <Badge color="failure">{t('admin.userManagement.inactive')}</Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    {user.admin ? (
                      <Badge color="purple">{t('admin.userManagement.admin')}</Badge>
                    ) : (
                      <Badge color="gray">{t('admin.userManagement.user')}</Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-2">
                      <Button size="xs" color="info" onClick={() => openDetails(user)}>
                        {t('common.details')}
                      </Button>
                      <Button
                        size="xs"
                        color={user.active ? 'warning' : 'success'}
                        onClick={() => handleToggleActive(user)}
                      >
                        {user.active ? t('admin.userManagement.deactivate') : t('admin.userManagement.activate')}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Modal show={isDetailsModalOpen} onClose={() => setIsDetailsModalOpen(false)}>
        <ModalHeader>{t('admin.userManagement.userDetails')}</ModalHeader>
        <ModalBody>
          {selectedUser && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>{t('user.fullName')}</Label>
                  <div className="text-lg font-semibold">{selectedUser.fullName}</div>
                </div>
                <div>
                  <Label>{t('user.email')}</Label>
                  <div className="text-lg">{selectedUser.email}</div>
                </div>
                <div>
                  <Label>{t('user.activeSchema')}</Label>
                  <div className="text-lg">{selectedUser.activeSchema || t('common.none')}</div>
                </div>
                <div>
                  <Label>{t('common.id')}</Label>
                  <div className="text-lg">{selectedUser.id}</div>
                </div>
              </div>

              <hr />

              <div className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <Label htmlFor="active-toggle">{t('admin.userManagement.activeAccount')}</Label>
                  <ToggleSwitch
                    id="active-toggle"
                    checked={!!selectedUser.active}
                    label=""
                    onChange={() => handleToggleActive(selectedUser)}
                  />
                </div>
                <div className="flex items-center justify-between">
                  <Label htmlFor="admin-toggle">{t('admin.userManagement.adminPrivileges')}</Label>
                  <ToggleSwitch
                    id="admin-toggle"
                    checked={!!selectedUser.admin}
                    label=""
                    onChange={() => handleToggleAdmin(selectedUser)}
                  />
                </div>
              </div>
            </div>
          )}
        </ModalBody>
        <ModalFooter>
          <Button color="gray" onClick={() => setIsDetailsModalOpen(false)}>
            {t('common.close')}
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  )
}
