package com.yahorbarkouski.recent

import com.intellij.ide.ProjectGroup
import com.intellij.ide.RecentProjectListActionProvider
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.ReopenProjectAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame

class GroupRecentProjectsByFolderAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val recentProjectsManager = RecentProjectsManager.getInstance()
        val recentProjectFolders = fetchRecentProjectFolders()

        val movingProjectsCount = calculateMovingProjectsCount(recentProjectsManager, recentProjectFolders)

        val response = Messages.showYesNoDialog(
            e.project,
            "This action is going to move $movingProjectsCount projects " +
                    "to new recent project groups. Do you want to proceed?",
            "Confirmation",
            Messages.getWarningIcon()
        )

        if (response == Messages.YES) {
            createOrUpdateGroups(recentProjectsManager, recentProjectFolders)
        }
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = calculateMovingProjectsCount(
            RecentProjectsManager.getInstance(),
            recentFoldersWithProjects = fetchRecentProjectFolders()
        ) > 0
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    private fun fetchRecentProjectFolders() =
        RecentProjectListActionProvider.getInstance()
            .getActions()
            .mapNotNull { (it as? ReopenProjectAction)?.projectPath }
            .groupBy { it.substringBeforeLast("/").substringAfterLast("/") }

    private fun calculateMovingProjectsCount(
        recentProjectsManager: RecentProjectsManager,
        recentFoldersWithProjects: Map<String, List<String>>
    ): Int {
        return recentFoldersWithProjects.keys.count { folder ->
            recentProjectsManager.groups.find { it.name == folder }?.projects != recentFoldersWithProjects[folder]
        }
    }

    private fun createOrUpdateGroups(
        recentProjectsManager: RecentProjectsManager,
        recentProjectFolders: Map<String, List<String>>
    ) {
        recentProjectFolders.map { (folder, projects) ->
            recentProjectsManager.groups.find { it.name == folder }?.apply {
                this.projects = projects
            } ?: ProjectGroup(folder).apply {
                this.projects = projects
                recentProjectsManager.addGroup(this)
            }
        }
    }
}
