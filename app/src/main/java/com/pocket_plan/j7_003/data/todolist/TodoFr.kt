package com.pocket_plan.j7_003.data.todolist

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pocket_plan.j7_003.MainActivity
import com.pocket_plan.j7_003.R
import com.pocket_plan.j7_003.R.*
import com.pocket_plan.j7_003.data.fragmenttags.FT
import com.pocket_plan.j7_003.data.home.HomeFr
import com.pocket_plan.j7_003.data.settings.SettingId
import com.pocket_plan.j7_003.data.settings.SettingsManager
import kotlinx.android.synthetic.main.dialog_add_task.*
import kotlinx.android.synthetic.main.dialog_add_task.view.*
import kotlinx.android.synthetic.main.fragment_todo.view.*
import kotlinx.android.synthetic.main.row_task.view.*
import kotlinx.android.synthetic.main.title_dialog.view.*

/**
 * A simple [Fragment] subclass.
 */

class TodoFr : Fragment() {
    private lateinit var myMenu: Menu
    private lateinit var myActivity: MainActivity

    private lateinit var addTaskDialog: AlertDialog
    private lateinit var addTaskDialogView: View
    lateinit var myFragment: TodoFr

    companion object {
        //Displayed as middle priority 2, (0 indexed)
        var lastUsedTaskPriority = 1
        lateinit var myAdapter: TodoTaskAdapter
        lateinit var myRecycler: RecyclerView

        lateinit var todoListInstance: TodoList
        var deletedTasks = ArrayDeque<Task?>()

        var offsetTop: Int = 0
        var firstPos: Int = 0
        lateinit var layoutManager: LinearLayoutManager

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_tasks, menu)
        myMenu = menu
        myMenu.findItem(R.id.item_tasks_undo)?.icon?.setTint(myActivity.colorForAttr(attr.colorOnBackGround))
        updateTodoIcons()
        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.item_tasks_delete_checked -> {
                //delete checked tasks and update the undoTask icon
                val titleId = string.tasksDialogClearChecked
                val action: () -> Unit = {
                    myFragment.manageCheckedTaskDeletion()
                    myFragment.updateTodoIcons()
                }
                myActivity.dialogConfirm(titleId, action)
            }

            R.id.item_tasks_undo -> {
                //undo deletion of last task

                val newPos = todoListInstance.addFullTask(deletedTasks.last()!!)
                myAdapter.notifyItemInserted(newPos)
                deletedTasks.removeLast()

            }

            R.id.item_tasks_clear -> {
                //delete ALL tasks in list
                val titleId = string.tasksDialogClearList
                val action: () -> Unit = {
                    todoListInstance.clear()
                    myAdapter.notifyDataSetChanged()
                    todoListInstance.save()
                }
                myActivity.dialogConfirm(titleId, action)
            }

            R.id.item_tasks_uncheck_all -> {
                //uncheck all tasks
                todoListInstance.uncheckAll()
                myAdapter.notifyDataSetChanged()
            }

        }
        updateTodoIcons()
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        myActivity = activity as MainActivity
        val myView = inflater.inflate(layout.fragment_todo, container, false)
        myRecycler = myView.recycler_view_todo
        myFragment = this

        todoListInstance = TodoList()

        /**
         * Connecting Adapter, Layout-Manager and Swipe Detection to UI elements
         */

        myAdapter = TodoTaskAdapter(myActivity, this)
        myRecycler.adapter = myAdapter

        layoutManager = LinearLayoutManager(activity)
        myRecycler.layoutManager = layoutManager
        myRecycler.setHasFixedSize(true)

        //itemTouchHelper to drag and reorder notes
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                var previousPosition: Int = -1
                var moving = false

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    //get current position in adapter
                    val currentPosition = viewHolder.bindingAdapterPosition

                    //mark that moving has ended (to allow a new previousPosition when move is detected)
                    moving = false

                    // don't refresh item if
                    // currentPosition == -1   =>  clearView got called due to a swipe to delete
                    // currentPosition == previousPosition   =>  item was moved, but placed back to original position
                    // previousPosition == -1   =>  item was selected but not moved
                    if (currentPosition == -1 || currentPosition == previousPosition || previousPosition == -1) {
                        previousPosition = -1
                        super.clearView(recyclerView, viewHolder)
                        return
                    }

                    //save task that was moved
                    val movedTask = todoListInstance[previousPosition]
                    //remove it from its previous position
                    todoListInstance.removeAt(previousPosition)
                    //re-add it at the current adapter position
                    todoListInstance.add(currentPosition, movedTask)

                    //save old values
                    val oldPriority = movedTask.priority
                    val oldCheckedState = movedTask.isChecked

                    //initialize new values
                    val newPriority: Int
                    val newCheckedState: Boolean

                    //get new values for priority and checked state
                    if (currentPosition > previousPosition) {
                        //if moved down, take values from above
                        newPriority = todoListInstance[currentPosition - 1].priority
                        newCheckedState = todoListInstance[currentPosition - 1].isChecked
                    } else {
                        //if moved up, take values from below
                        newPriority = todoListInstance[currentPosition + 1].priority
                        newCheckedState = todoListInstance[currentPosition + 1].isChecked
                    }

                    //apply changes
                    movedTask.priority = newPriority
                    movedTask.isChecked = newCheckedState

                    //save changes
                    todoListInstance.save()

                    //notify change if priority or checkedState changed
                    if (oldPriority != newPriority || oldCheckedState != newCheckedState) {
                        myAdapter.notifyItemChanged(currentPosition)
                    }

                    //reset previousPosition to -1 to mark that nothing is moving
                    previousPosition = -1

                    //clear view
                    super.clearView(recyclerView, viewHolder)
                }


                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
                ): Boolean {

                    if (!moving) {
                        //if not moving, save new previous position
                        previousPosition = viewHolder.bindingAdapterPosition

                        //and prevent new previous positions from being set until this move is over
                        moving = true
                    }

                    //get start and end position of this move
                    val fromPos = viewHolder.bindingAdapterPosition
                    val toPos = target.bindingAdapterPosition

                    // animate move of task from `fromPos` to `toPos` in adapter.
                    myAdapter.notifyItemMoved(fromPos, toPos)

                    //indicates successful move
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    //get index where task should be deleted
                    val deletedAtIndex = viewHolder.bindingAdapterPosition

                    //save task at that index
                    deletedTasks.add(todoListInstance.getTask(deletedAtIndex))

                    //delete this task form todoListInstance
                    todoListInstance.deleteTask(deletedAtIndex)

                    //animate remove in adapter
                    myAdapter.notifyItemRemoved(deletedAtIndex)

                    //update menu icons
                    myFragment.updateTodoIcons()
                }
            })

        itemTouchHelper.attachToRecyclerView(myRecycler)

        return myView
    }


    fun updateTodoIcons() {
        updateUncheckTaskListIcon()
        updateClearTaskListIcon()
        updateUndoTaskIcon()
        updateDeleteCheckedTasksIcon()
    }

    fun updateUndoTaskIcon() {
        myMenu.findItem(R.id.item_tasks_undo)?.isVisible = deletedTasks.isNotEmpty()
    }

    private fun updateClearTaskListIcon() {
        myMenu.findItem(R.id.item_tasks_clear)?.isVisible = todoListInstance.size > 0
    }

    private fun updateUncheckTaskListIcon() {
        myMenu.findItem(R.id.item_tasks_uncheck_all)?.isVisible =
            todoListInstance.somethingIsChecked()
    }

    private fun updateDeleteCheckedTasksIcon() {
        myMenu.findItem(R.id.item_tasks_delete_checked)?.isVisible =
            todoListInstance.somethingIsChecked()
    }

    private fun updateDeleteTaskIcon() {
        val checkedTasks = todoListInstance.filter { t -> t.isChecked }.size
        myMenu.findItem(R.id.item_tasks_delete_checked)?.isVisible = checkedTasks > 0
    }

    fun prepareForMove() {
        firstPos = layoutManager.findFirstVisibleItemPosition()
        offsetTop = 0
        if (firstPos >= 0) {
            val firstView = layoutManager.findViewByPosition(firstPos)
            offsetTop =
                layoutManager.getDecoratedTop(firstView!!) - layoutManager.getTopDecorationHeight(
                    firstView
                )
        }
    }

    fun reactToMove() {
        layoutManager.scrollToPositionWithOffset(
            firstPos,
            offsetTop
        )
    }

    //Deletes all checked tasks and animates the deletion
    private fun manageCheckedTaskDeletion() {
        deletedTasks.clear()
        val oldSize = todoListInstance.size
        val newSize = todoListInstance.deleteCheckedTasks()
        myAdapter.notifyItemRangeRemoved(newSize, oldSize)
        updateDeleteTaskIcon()
    }

    fun preloadAddTaskDialog(passedActivity: MainActivity, myLayoutInflater: LayoutInflater){
        myActivity = passedActivity
        //inflate the dialog with custom view
        addTaskDialogView =
            myLayoutInflater.inflate(layout.dialog_add_task, null)

        //AlertDialogBuilder
        val myBuilder =
            myActivity.let { it1 -> AlertDialog.Builder(it1).setView(addTaskDialogView) }
        myBuilder?.setCustomTitle(
            myLayoutInflater.inflate(
                layout.title_dialog,
                null
            )
        )

        //show dialog
        addTaskDialog = myBuilder?.create()!!
        addTaskDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        //adds listeners to confirmButtons in addTaskDialog
        val taskConfirmButtons = arrayListOf<Button>(
            addTaskDialogView.btnConfirm1,
            addTaskDialogView.btnConfirm2,
            addTaskDialogView.btnConfirm3
        )

        addTaskDialogView.etTitleAddTask.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                taskConfirmButtons[lastUsedTaskPriority].performClick()
                true
            } else false
        }

        taskConfirmButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val title = addTaskDialogView.etTitleAddTask.text.toString()
                addTaskDialog.etTitleAddTask.setText("")
                if (title.trim().isEmpty()) {
                    val animationShake =
                        AnimationUtils.loadAnimation(myActivity, anim.shake)
                    addTaskDialogView.etTitleAddTask.startAnimation(animationShake)
                    @Suppress("LABEL_NAME_CLASH")
                    return@setOnClickListener
                }
                lastUsedTaskPriority = index
                val newPos =
                    todoListInstance.addFullTask(
                        Task(
                            title,
                            index + 1,
                            false
                        )
                    )

                addTaskDialog.dismiss()

                if(MainActivity.previousFragmentStack.peek() == FT.HOME){
                    val homeFr = myActivity.getFragment(FT.HOME) as HomeFr
                    homeFr.updateTaskPanel(false)
                    myActivity.toast(myActivity.getString(string.homeNotificationTaskAdded))
                    return@setOnClickListener
                }

                myRecycler.adapter?.notifyItemInserted(newPos)
                myRecycler.scrollToPosition(newPos)
                myFragment.updateTodoIcons()
            }
        }
    }

    @SuppressLint("InflateParams")
    fun dialogAddTask() {
        addTaskDialog.show()
        addTaskDialogView.etTitleAddTask.requestFocus()
    }
}

class TodoTaskAdapter(activity: MainActivity, var myFragment: TodoFr) :
    RecyclerView.Adapter<TodoTaskAdapter.TodoTaskViewHolder>() {
    private val myActivity = activity
    private val listInstance = TodoFr.todoListInstance
    private val round = SettingsManager.getSetting(SettingId.SHAPES_ROUND) as Boolean
    private val dark = SettingsManager.getSetting(SettingId.THEME_DARK) as Boolean
    private val cr = myActivity.resources.getDimension(dimen.cornerRadius)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoTaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(layout.row_task, parent, false)
        return TodoTaskViewHolder(itemView)
    }


    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: TodoTaskViewHolder, position: Int) {

        holder.itemView.visibility = View.VISIBLE

        val currentTask = listInstance.getTask(holder.bindingAdapterPosition)

        //Set text of task to be visible
        holder.itemView.tvName.text = currentTask.title

        //Set Long click listener to initiate re-sorting
        holder.itemView.tvName.setOnLongClickListener {
            val animationShake =
                AnimationUtils.loadAnimation(myActivity, anim.shake_small)
            holder.itemView.startAnimation(animationShake)
            true
        }

        if (currentTask.isChecked) {
            //Display the task as checked: check checkbox, strike through text, use gray colors for text and background
            holder.itemView.cbTask.isChecked = true
            holder.itemView.tvName.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.tvName.setTextColor(
                myActivity.colorForAttr(attr.colorHint)
            )
            holder.itemView.crvTask.setCardBackgroundColor(myActivity.colorForAttr(attr.colorCheckedTask))
            holder.itemView.crvBg.setCardBackgroundColor(myActivity.colorForAttr(attr.colorCheckedTask))
        } else {
            //Display the task as unchecked: Uncheck checkbox, remove strike-through of text, initialize correct colors
            holder.itemView.cbTask.isChecked = false
            holder.itemView.tvName.paintFlags = 0

            val taskTextColor = if (dark) {
                //colored task text when in dark theme
                if (SettingsManager.getSetting(SettingId.DARK_BORDER_STYLE) == 3.0)
                    attr.colorOnBackGround
                else when (listInstance.getTask(holder.bindingAdapterPosition).priority) {
                        1 -> attr.colorPriority1
                        2 -> attr.colorPriority2
                        else -> attr.colorPriority3
                    }
            } else {
                //white text when in light theme
                attr.colorBackground
            }

            val taskBackgroundColor = if (dark) {
                //dark background in dark theme
                if (SettingsManager.getSetting(SettingId.DARK_BORDER_STYLE) != 3.0)
                    attr.colorBackgroundElevated
                else when (listInstance.getTask(holder.bindingAdapterPosition).priority) {
                        1 -> attr.colorPriority1darker
                        2 -> attr.colorPriority2darker
                        else -> attr.colorPriority3darker
                    }
            } else {
                //colored background in light theme
                when (listInstance.getTask(holder.bindingAdapterPosition).priority) {
                    1 -> attr.colorPriority1
                    2 -> attr.colorPriority2
                    else -> attr.colorPriority3
                }
            }

            val taskBorderColor = if (dark) {
                when (SettingsManager.getSetting(SettingId.DARK_BORDER_STYLE)) {
                    1.0 -> attr.colorBackgroundElevated
                    2.0 -> taskTextColor
                    else -> taskBackgroundColor
                }
            } else {
                taskBackgroundColor
            }

            holder.itemView.tvName.setTextColor(myActivity.colorForAttr(taskTextColor))
            holder.itemView.crvTask.setCardBackgroundColor(myActivity.colorForAttr(taskBackgroundColor))
            holder.itemView.crvBg.setCardBackgroundColor(myActivity.colorForAttr(taskBorderColor))
        }

        //set corner radius to be round if style is set to round
        holder.itemView.crvTask.radius = if (round) cr else 0f
        holder.itemView.crvBg.radius = if (round) cr else 0f

        /**
         * EDITING task
         * Onclick-Listener on List items, opening the edit-task dialog
         */

        holder.itemView.tvName.setOnClickListener {

            //inflate the dialog with custom view
            val myDialogView = LayoutInflater.from(myActivity).inflate(
                layout.dialog_add_task,
                null
            )

            //AlertDialogBuilder
            val myBuilder = AlertDialog.Builder(myActivity).setView(myDialogView)
            val editTitle = LayoutInflater.from(myActivity).inflate(
                layout.title_dialog,
                null
            )
            editTitle.tvDialogTitle.text = myActivity.resources.getText(string.tasksEditTitle)
            myBuilder.setCustomTitle(editTitle)

            //show dialog
            val myAlertDialog = myBuilder.create()
            myAlertDialog.window?.setSoftInputMode(
                WindowManager
                    .LayoutParams.SOFT_INPUT_STATE_VISIBLE
            )
            myAlertDialog.show()

            //write current task to textField
            myDialogView.etTitleAddTask.requestFocus()
            myDialogView.etTitleAddTask.setText(listInstance.getTask(holder.bindingAdapterPosition).title)
            myDialogView.etTitleAddTask.setSelection(myDialogView.etTitleAddTask.text.length)

            //adds listeners to confirmButtons in addTaskDialog
            val taskConfirmButtons = arrayListOf<Button>(
                myDialogView.btnConfirm1,
                myDialogView.btnConfirm2,
                myDialogView.btnConfirm3
            )

            myDialogView.etTitleAddTask.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                    taskConfirmButtons[listInstance.getTask(holder.bindingAdapterPosition).priority-1].performClick()
                    true
                } else false
            }

            //Three buttons to create tasks with priorities 1-3
            taskConfirmButtons.forEachIndexed { index, button ->
                button.setOnClickListener Button@{
                    if (myDialogView.etTitleAddTask.text.toString().trim() == "") {
                        val animationShake =
                            AnimationUtils.loadAnimation(myActivity, anim.shake)
                        myDialogView.etTitleAddTask.startAnimation(animationShake)
                        return@Button
                    }
                    val newPos = listInstance.editTask(
                        holder.bindingAdapterPosition, index + 1,
                        myDialogView.etTitleAddTask.text.toString(),
                        listInstance.getTask(holder.bindingAdapterPosition).isChecked
                    )
                    this.notifyItemChanged(holder.bindingAdapterPosition)
                    myFragment.prepareForMove()
                    this.notifyItemMoved(holder.bindingAdapterPosition, newPos)
                    myFragment.reactToMove()
                    myAlertDialog.dismiss()

                }
            }
        }

        //reacts to the user checking a task
        holder.itemView.tapField.setOnClickListener {
            val checkedStatus = !listInstance.getTask(holder.bindingAdapterPosition).isChecked
            holder.itemView.cbTask.isChecked = checkedStatus
            val task = listInstance.getTask(holder.bindingAdapterPosition)
            val newPos = listInstance.editTask(
                holder.bindingAdapterPosition, task.priority,
                task.title, checkedStatus
            )
            myFragment.updateUndoTaskIcon()

            notifyItemChanged(holder.bindingAdapterPosition)
            if (holder.bindingAdapterPosition != newPos) {
                myFragment.prepareForMove()
                notifyItemMoved(holder.bindingAdapterPosition, newPos)
                myFragment.reactToMove()
            }
            myFragment.updateTodoIcons()

        }
    }

    override fun getItemCount() = TodoFr.todoListInstance.size

    class TodoTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}



