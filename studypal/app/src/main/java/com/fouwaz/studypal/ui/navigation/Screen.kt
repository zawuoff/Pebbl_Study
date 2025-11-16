package com.fouwaz.studypal.ui.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object ProjectList : Screen("project_list")
    object NewProject : Screen("new_project")
    object VoiceSession : Screen("voice_session/{projectId}") {
        fun createRoute(projectId: Long) = "voice_session/$projectId"
    }
    object DraftView : Screen("draft_view/{projectId}") {
        fun createRoute(projectId: Long) = "draft_view/$projectId"
    }
    object PebbleCollection : Screen("pebble_collection")
    object LectureList : Screen("lecture_list")
    object LectureRecording : Screen("lecture_recording?projectId={projectId}") {
        fun createRoute(projectId: Long?): String {
            return if (projectId != null) {
                "lecture_recording?projectId=$projectId"
            } else {
                "lecture_recording"
            }
        }
    }
    object LectureOutput : Screen("lecture_output/{lectureId}") {
        fun createRoute(lectureId: Long) = "lecture_output/$lectureId"
    }
}

