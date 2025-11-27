## Trinity College Scheduler Frontend – Architecture & Structure

### Overview

The frontend is a **React + TypeScript** single‑page application built with **Vite** and **Material UI (MUI)**. It provides two primary experiences:

- **Admin Dashboard** – for generating and inspecting the master semester schedule.
- **Student Planner** – for viewing a student’s personal schedule and enrolling in available sections.

Data fetching is handled by **@tanstack/react-query**, and API calls are centralized in `services/api.ts`. Scheduling data is visualized using **FullCalendar**.

### Application Shell & Routing

- **`App.tsx`**
  - Sets up global providers:
    - `QueryClientProvider` for React Query.
    - `ThemeProvider` with a custom MUI theme.
    - `CssBaseline` for a consistent base style.
  - Configures routing with `react-router-dom`:
    - `/admin` → `AdminDashboard`
    - `/student` and `/student/:studentId` → `StudentPlanner`
    - `/` redirects to `/admin`.
  - Wraps all routes with the `Layout` component to provide a consistent header and footer.

- **`Layout.tsx`**
  - Shared frame for all pages:
    - **`AppBar`** with:
      - App title: “Trinity College - Course Scheduler”.
      - Navigation buttons:
        - **Admin** → `/admin`
        - **Student** → `/student`
      - Highlights the active section based on the current route.
    - **`Container`** for page content.
    - **Footer** with application copyright.

### Data Layer & API Access

- **`services/api.ts`**
  - Centralized Axios client configured with:
    - `baseURL` from `VITE_API_BASE_URL` (defaults to `http://localhost:8080`).
    - JSON content type headers.
    - Request and response interceptors for logging and error reporting.
  - Defines strongly‑typed API calls that mirror backend endpoints:
    - **Schedule generation & retrieval**
      - `generateSchedule(semesterId)` → `POST /api/semesters/{semesterId}/schedule`
      - `getSchedule(semesterId)` → `GET /api/semesters/{semesterId}`
    - **Sections**
      - `getSections(semesterId, status?, courseId?)` → `GET /api/sections` with query params.
    - **Enrollment**
      - `validateEnrollment(studentId, sectionId)` → `GET /api/students/{studentId}/enroll/validate`
      - `enrollStudent(request)` → `POST /api/students/{studentId}/enroll`
    - **Student schedule & progress**
      - `getStudentSchedule(studentId, semesterId)` → `GET /api/students/{studentId}/schedule`
      - `getAcademicProgress(studentId)` → `GET /api/students/{studentId}/progress`

- **`hooks/useSchedule.ts`**
  - Wraps `services/api.ts` with **React Query hooks** to provide caching, loading states, and automatic refetch:
    - `useGenerateScheduleV1()` – mutation for generating a schedule:
      - On success, invalidates `['schedule', semesterId]` and `['sections', semesterId]`.
    - `useSchedule(semesterId)` – query for master semester schedule.
    - `useSections(semesterId, status?, courseId?)` – query for available course sections.
    - `useValidateEnrollment(studentId, sectionId)` – query for pre‑enrollment validation.
    - `useEnrollStudent()` – mutation for enrolling:
      - On success, invalidates:
        - `['student-schedule', studentId]`
        - `['sections']`
        - `['academic-progress', studentId]`
    - `useStudentSchedule(studentId, semesterId)` – query for a student’s personal schedule.
    - `useAcademicProgress(studentId)` – query for high‑level student progress info.

This separation (API module + hooks) keeps components lean and focused on UI logic.

### UI Pages

#### Admin Dashboard – `pages/AdminDashboard.tsx`

- **Purpose**: Provide an interface for administrators to generate and review the master schedule for a given semester.
- **Key state**:
  - `semesterId` / `semesterName` – selected semester.
  - `refreshSchedule` – flag to control when the schedule query is enabled.
  - `schedule` – currently displayed `ScheduleResponse`.
- **Data dependencies**:
  - `useGenerateScheduleV1()` – generates a new schedule.
  - `useSchedule(semesterId)` – fetches the existing schedule for display.
- **Main UI sections**:
  - **Left Panel – “Generate Schedule”**
    - MUI `Select` for choosing `semesterId` (options from `semesterList`).
    - `Generate Schedule` button:
      - Calls the generate mutation with the selected `semesterId`.
      - Shows loading state and success/error alerts.
      - Shows summary of scheduled vs created sections and success rate.
    - `Refresh` button:
      - Toggles or triggers a refetch of the schedule (`useSchedule`).
  - **Right Panel – “Master Schedule”**
    - Displays a `ScheduleCalendar`:
      - `sections` from the `ScheduleResponse`.
      - `semesterStartDate` / `semesterEndDate`.
    - Handles three states:
      - Loading (spinner).
      - Schedule present → calendar view.
      - No schedule → informational alert prompting generation.

The Admin Dashboard is essentially a **control panel** around the backend scheduling engine.

#### Student Planner – `pages/StudentPlanner.tsx`

- **Purpose**: Let a student view their personal schedule, see available sections, and enroll in courses subject to backend rules.
- **Key state**:
  - `studentId` – from the URL (`/student/:studentId`) or user input.
  - `semesterId` – selected semester.
  - `selectedSection` – currently selected section for enrollment.
  - `enrollDialogOpen` / `enrollmentStatus` – controls and feedback for the enrollment dialog.
- **Data dependencies**:
  - `useSections(semesterId, 'SCHEDULED')` – list of available sections for the semester.
  - `useStudentSchedule(studentId, semesterId)` – student’s enrolled sections + semester dates.
  - `useAcademicProgress(studentId)` – high‑level course completion info.
  - `useEnrollStudent()` – mutation for enrollment.
- **Main UI sections**:
  - **Left Column**
    - Student controls:
      - `TextField` for `studentId`.
      - Semester `Select` for choosing `semesterId`.
    - **Student summary card**:
      - Student name and grade level.
      - Chips showing:
        - Completed courses count.
        - Currently enrolled courses count.
    - **“My Schedule” list**:
      - List of enrolled sections with course name, section number, teacher, and hours/week.
  - **Right Column**
    - **Personal calendar**:
      - `ScheduleCalendar` showing only the student’s enrolled sections.
    - **“Available Courses” grid**:
      - Card per `Section` with:
        - Course name, section number.
        - Teacher and classroom.
        - Weekly hours and a summary of day/time slots.
      - `Enroll` button (or `Full`/`Enrolled` badges depending on capacity/enrollment).
    - **Enrollment confirmation dialog**:
      - Shows full details of the selected section (teacher, room, hours/week, detailed schedule).
      - Uses `useEnrollStudent` to call the backend.
      - Displays success or backend‑provided error message (e.g. time conflicts, prerequisites not met, course load limits).

The Student Planner deliberately offloads all business logic (conflicts, prerequisites, capacity) to the backend and only displays responses.

### Shared Components & Utilities

- **`components/ScheduleCalendar.tsx`**
  - Wrapper around `FullCalendar` to render schedules for:
    - Master schedule (admin view).
    - Personal schedule (student view).
  - Accepts:
    - `sections` – list of `Section` models (flattened from backend).
    - `semesterStartDate` / `semesterEndDate`.
    - Optional `title`, `initialView`, and `height`.
  - Uses `sectionsToCalendarEvents` from `utils/calendarUtils` to transform sections into FullCalendar events.
  - Configuration:
    - Uses `timeGridWeek` or `dayGridMonth` views.
    - Working hours 09:00–17:00, weekdays only.
    - Custom header showing semester range.
  - Custom `eventContent`:
    - Shows up to two small chips per cell representing overlapping events.
    - If there are more than two events, renders a “+N more” chip that opens a **modal**.
  - **Modal behavior**:
    - Uses `TablePopup` to show all events in the selected time slot in tabular form.
    - Displays total number of slots.

- **`components/Layout.tsx`**
  - Already covered under “Application Shell & Routing”.
  - Provides a consistent outer frame for all pages.

- **`components/TablePopup.tsx`**
  - (Not fully shown above but implied by usage in `ScheduleCalendar`).
  - Renders the details of multiple `CalendarEvent`s in a tabular layout for the modal.

- **`utils/calendarUtils.ts`**
  - `semesterList` – static list of semester IDs and user‑friendly labels used in the admin and student pages.
  - `sectionsToCalendarEvents(sections, semesterStartDate)` – maps backend Section/Timeslot structures to FullCalendar event objects.
  - `formatTimeRange`, `getDayName` – formatting helpers for displaying schedules.

### Types & Data Contracts

- **`types/index.ts`**
  - Contains all TypeScript interfaces and types used across the app, including:
    - `ScheduleResponse` – matches backend `ScheduleResponse`.
    - `Section` – flattened section model used by the UI calendar and cards.
    - `EnrollmentRequest`, `EnrollmentResponse`, `ValidationResponse`.
    - `StudentSchedule`, `AcademicProgress`.
    - `CalendarEvent` – internal representation for FullCalendar.
  - These types mirror the backend DTOs and ensure **type‑safe integration** between frontend and backend.

### How Everything Fits Together

- **Admin flow**
  - User navigates to `/admin`.
  - Chooses a semester → clicks **Generate Schedule**:
    - `useGenerateScheduleV1` calls backend `POST /api/semesters/{semesterId}/schedule`.
    - On success, React Query invalidates relevant queries.
  - `useSchedule(semesterId)` re‑fetches the updated schedule.
  - `ScheduleCalendar` renders the result as a week view.

- **Student flow**
  - User navigates to `/student` or `/student/:studentId`.
  - Enters/selects `studentId` and `semesterId`.
  - React Query hooks load:
    - Academic progress for the student.
    - Personal schedule for the selected semester.
    - Available (scheduled) sections in that semester.
  - Student clicks **Enroll** on a section:
    - Enrollment dialog shows details.
    - Confirm calls `useEnrollStudent` → backend enrollment endpoint.
    - On success, React Query invalidates schedule/sections/progress so the UI updates automatically.
    - On error, the dialog shows the backend‑provided message explaining why the enrollment failed.

The frontend is intentionally **thin in business logic**: it assumes the backend owns all rules about scheduling, eligibility, and conflicts, and focuses instead on a clean user experience, rich visualizations, and responsive feedback based on backend responses.


