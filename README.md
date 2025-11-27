# Trinity College Course Scheduler - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites
- Java 17+
- Node.js 20+ (for frontend)
- Git Bash (Windows) or Bash (Linux/Mac)

### Option A: Using the Web UI (Recommended)

#### Step 1: Start the Backend

```bash
cd backend
./gradlew bootRun
```

Wait for: `Started Application in few seconds`

#### Step 2: Start the Frontend

In a new terminal:

```bash
cd frontend
npm install
npm run dev
```

Wait for: `Local: http://localhost:3000`

## 🐳 Using Docker

### Start with Docker (Backend + Frontend)

```bash
docker compose up --build
```

This starts both:
- **Backend**: http://localhost:8080
- **Frontend**: http://localhost:3000

### Stop

```bash
docker compose down
```

## 🧪 Run Tests

```bash
cd backend
./gradlew test
```

View test report: `build/reports/tests/test/index.html`

#### Step 3: Open the Web UI

Open in browser: **http://localhost:3000**

#### Step 4: Generate a Schedule (Admin Dashboard)

1. Navigate to **Admin Dashboard** (or go to http://localhost:3000/admin)
2. Enter `Semester ID`: 1
3. Click **"Generate Schedule"**
4. Wait 5-15 seconds for schedule generation
5. View the generated schedule in the interactive calendar!

#### Step 5: Enroll a Student (Student Planner)

1. Navigate to **Student Planner** (or go to http://localhost:3000/student)
2. Enter `Student ID`: 1
3. Select `Semester ID`: 1
4. Browse available courses in the grid
5. Click **"Enroll"** on any section
6. Confirm enrollment in the dialog
7. View your personal schedule in the calendar!

### Option B: Using Swagger UI (API Testing)

#### Step 1: Start the Backend

```bash
cd backend
./gradlew bootRun
```

Wait for: `Started TrinitySchedulerApplication in X seconds`

#### Step 2: Open Swagger UI

Open in browser: http://localhost:8080/swagger-ui.html

#### Step 3: Generate a Schedule

In Swagger UI:
1. Expand `Semester Schedule Generation`
2. Click `POST /api/semesters/{semesterId}/schedule`
3. Click "Try it out"
4. Set parameters:
   - `semesterId`: 1
5. Click "Execute"

Wait 5-15 seconds for schedule generation.

#### Step 4: View the Schedule

1. Expand `Semester Schedule Generation`
2. Click `GET /api/semesters/{semesterId}`
3. Click "Try it out"
4. Set `semesterId`: 1
5. Click "Execute"

You'll see all scheduled sections with timeslots!

#### Step 5: Enroll a Student

1. Click `POST /api/students/{studentId}/enroll`
2. Click "Try it out"
3. Enter request body:
```json
{
  "studentId": 1,
  "sectionId": 1
}
```
4. Set `studentId`: 1
5. Click "Execute"

Check the response for enrollment status!

## 📖 Key Endpoints

### Schedule Generation
- **POST** `/api/semesters/{semesterId}/schedule`
  - Generates master schedule for a semester

### View Schedule
- **GET** `/api/semesters/{semesterId}`
  - View all sections and timeslots for a semester

### List Sections
- **GET** `/api/sections?semesterId=1`
  - List all available sections for a semester

### Validate Student Enrollment
- **GET** `/api/students/{studentId}/enroll/validate?sectionId=1`
  - Validate if a student can enroll in a section (conflicts, prerequisites, capacity, load)

### Enroll Student
- **POST** `/api/students/{studentId}/enroll`
  - Enroll student in a section

### Student Schedule
- **GET** `/api/students/{studentId}/schedule?semesterId=1`
  - View a student's enrolled courses for a semester

### Student Academic Progress
- **GET** `/api/students/{studentId}/progress`
  - View a student's academic progress

## 🎨 Web UI Features

The Trinity College Scheduler includes a modern React-based web interface with two main views:

### Admin Dashboard (`/admin`)
- **Schedule Generation**: Generate master schedules for semesters
- **Interactive Calendar**: Visual representation of all scheduled sections
- **Statistics**: View success rates and execution times
- **Run History**: Track past schedule generation attempts

### Student Planner (`/student`)
- **Personal Schedule**: Calendar view of enrolled courses
- **Course Browser**: Grid of available sections with details
- **Enrollment**: One-click enrollment with validation
- **Academic Progress**: Track completed courses and prerequisites
- **Conflict Detection**: Automatic validation before enrollment

### UI Technology Stack
- **React 18** with TypeScript
- **Material-UI (MUI)** for modern components
- **FullCalendar** for interactive calendar views
- **React Query** for real-time data updates
- **Responsive Design** for mobile and desktop


## 📚 Documentation

- **Backend Architecture & Logic**: `BACKEND_ARCHITECTURE.md`
- **Frontend Architecture & Structure**: `FRONTEND_ARCHITECTURE.md`
- **Challenge Brief**: `challenge/README.md`
- **Database Details**: `challenge/DATABASE.md`

## 🔍 Useful URLs

### Frontend (Web UI)
- **Web Application**: http://localhost:3000
- **Admin Dashboard**: http://localhost:3000/admin
- **Student Planner**: http://localhost:3000/student

### Backend (API)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs JSON**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics

## 💡 Example cURL Commands

### Generate Schedule
```bash
curl -X POST "http://localhost:8080/api/semesters/1/schedule"
```

### View Schedule
```bash
curl "http://localhost:8080/api/semesters/1"
```

### Enroll Student
```bash
curl -X POST "http://localhost:8080/api/students/1/enroll" \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "sectionId": 1}'
```

### View Student Schedule
```bash
curl "http://localhost:8080/api/students/1/schedule?semesterId=1"
```

### View Student Academic Progress
```bash
curl "http://localhost:8080/api/students/1/progress"
```

## ❓ Troubleshooting

### Backend won't start
- Check Java version: `java -version` (should be 17+)
- Check port 8080 is free: `netstat -ano | findstr :8080` (Windows) or `lsof -i :8080` (Mac/Linux)

### Frontend won't start
- Check Node.js version: `node -version` (should be 20+)
- Check port 3000 is free: `netstat -ano | findstr :3000` (Windows) or `lsof -i :3000` (Mac/Linux)
- Install dependencies: `cd frontend && npm install`
- Ensure backend is running on port 8080

### Database errors
- Ensure `backend/database/trinity_college.sqlite` exists (this is the main SQLite database used by the app and Docker)
- Check file permissions and that Docker (if used) can read/write the file

### Tests failing
- Clean and rebuild: `./gradlew clean test`

## 📞 Need Help?

See detailed documentation in:
- `BACKEND_ARCHITECTURE.md` - Backend design, entities, and scheduling/enrollment logic
- `FRONTEND_ARCHITECTURE.md` - Frontend structure, routing, data layer, and UI components
- `challenge/README.md` - Original problem statement and constraints
- `challenge/DATABASE.md` - Database schema and sample data details

---

**That's it! You're ready to use the Trinity Scheduler!** 🎉

